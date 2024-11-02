package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.SitesList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private static final int TIMEOUT = 10000;
    private static final int ERROR_STATUS_CODE = -1;

    @Getter
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    // Инжектируем себя как прокси через квалификатор
    private final IndexingService self;

    @Value("${crawler.user-agent:Mozilla/5.0}")
    private String userAgent;

    @Value("${crawler.referrer:http://www.google.com}")
    private String referrer;

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

    public void startFullIndexing() {
        if (indexingInProgress.getAndSet(true)) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        stopIndexing.set(false);
        sitesList.getSites().forEach(siteConfig -> {
            Site site = clearOldDataForSite(siteConfig);
            log.info("Запущена индексация нового сайта: {}", site.getUrl());
            self.indexSiteAsync(site); // Используем прокси для вызова асинхронного метода
        });
    }

    private Site clearOldDataForSite(searchengine.config.Site siteConfig) {
        siteRepository.findByUrl(siteConfig.getUrl()).ifPresent(existingSite -> {
            pageRepository.deleteBySiteId(existingSite.getId());
            siteRepository.delete(existingSite);
            log.info("Удалены старые данные для сайта: {}", existingSite.getUrl());
        });
        Site site = new Site(siteConfig.getUrl(), siteConfig.getName(), Status.INDEXING, LocalDateTime.now());
        siteRepository.save(site);
        return site;
    }

    @Async
    public void indexSiteAsync(Site site) {
        if (stopIndexing.get()) {
            updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            return;
        }
        indexSite(site);
    }

    private void indexSite(Site site) {
        updateSiteStatus(site, Status.INDEXING, null);
        log.info("Индексация сайта: {}", site.getUrl());

        try {
            Document doc = fetchPageContent(site.getUrl());
            savePage(site, doc);
            updateSiteStatus(site, Status.INDEXED, null);
            log.info("Индексация сайта завершена успешно: {}", site.getUrl());
        } catch (IOException e) {
            handleIndexingError(site, e);
        }
    }

    private Document fetchPageContent(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(TIMEOUT)
                .get();
    }

    private void savePage(Site site, Document doc) {
        String url = site.getUrl();
        int statusCode = getStatusCode(url);
        if (!pageRepository.existsBySiteAndPath(site, url)) {
            Page page = new Page(site, url, statusCode, doc.html());
            pageRepository.save(page);
            log.info("Сохранена страница: {} для сайта: {}", url, site.getUrl());
        }
    }

    private int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.HEAD)
                    .timeout(TIMEOUT)
                    .execute();
            return response.statusCode();
        } catch (IOException e) {
            log.error("Ошибка при получении статуса для URL {}: {}", url, e.getMessage());
            return ERROR_STATUS_CODE;
        }
    }

    private void updateSiteStatus(Site site, Status status, String lastError) {
        site.updateStatus(status);
        if (lastError != null) {
            site.setLastError(lastError);
        }
        siteRepository.save(site);
        log.info("Обновлен статус сайта: {} на {}", site.getUrl(), status);
    }

    public void stopIndexing() {
        stopIndexing.set(true);
        log.info("Запрос на остановку индексации поступил.");
    }

    private void handleIndexingError(Site site, Exception e) {
        String errorMessage = "Ошибка индексации сайта " + site.getUrl() + ": " + e.getMessage();
        updateSiteStatus(site, Status.FAILED, errorMessage);
        log.error(errorMessage, e);
    }
}