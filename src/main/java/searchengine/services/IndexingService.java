package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.SitesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private static final int TIMEOUT = 10000; // Таймаут
    private static final int ERROR_STATUS_CODE = -1; // Код ошибки

    @Getter
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    private final ExecutorService siteIndexingExecutor = Executors.newCachedThreadPool();

    @Value("${crawler.user-agent:Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6}")
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
            Site existingSite = siteRepository.findByUrl(siteConfig.getUrl()).orElse(null);

            if (existingSite != null) {
                pageRepository.deleteBySiteId(existingSite.getId());
                siteRepository.delete(existingSite);
                logger.info("Удалены старые данные для сайта: {}", existingSite.getUrl());
            }

            Site site = createSiteFromConfig(siteConfig);
            siteRepository.save(site);
            logger.info("Запущена индексация нового сайта: {}", site.getUrl());

            siteIndexingExecutor.submit(() -> indexSite(site));
        });
    }

    private Site createSiteFromConfig(searchengine.config.Site configSite) {
        Site modelSite = new Site();
        modelSite.setUrl(configSite.getUrl());
        modelSite.setName(configSite.getName());
        modelSite.setStatus(Status.INDEXING);
        modelSite.setStatusTime(LocalDateTime.now());
        return modelSite;
    }

    private void indexSite(Site site) {
        if (stopIndexing.get()) {
            updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            return;
        }

        updateSiteStatus(site, Status.INDEXING, null);
        logger.info("Индексация сайта: {}", site.getUrl());

        try {
            Document doc = fetchPageContent(site.getUrl());
            savePage(site, doc);
            updateSiteStatus(site, Status.INDEXED, null);
            logger.info("Индексация сайта завершена успешно: {}", site.getUrl());
        } catch (IOException | InterruptedException e) {
            handleIndexingError(site, e);
        }
    }

    private Document fetchPageContent(String url) throws IOException, InterruptedException {
        Thread.sleep(10000); // Задержка на 10 секунд
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
            logger.info("Сохранена страница: {} для сайта: {}", url, site.getUrl());
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
            logger.error("Ошибка при получении статуса для URL {}: {}", url, e.getMessage());
            return ERROR_STATUS_CODE;
        }
    }

    private void updateSiteStatus(Site site, Status status, String lastError) {
        site.setStatus(status);
        if (lastError != null) {
            site.setLastError(lastError);
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        logger.info("Обновлен статус сайта: {} на {}", site.getUrl(), status);
    }

    public void stopIndexing() {
        stopIndexing.set(true);
        logger.info("Запрос на остановку индексации поступил.");
        siteIndexingExecutor.shutdownNow();

        siteRepository.findAll().forEach(site -> {
            if (!pageRepository.existsBySiteId(site.getId())) {
                updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            }
        });
    }

    private void handleIndexingError(Site site, Exception e) {
        String errorMessage = "Ошибка индексации сайта " + site.getUrl() + ": " + e.getMessage();
        updateSiteStatus(site, Status.FAILED, errorMessage);
        logger.error(errorMessage, e);
    }
}
