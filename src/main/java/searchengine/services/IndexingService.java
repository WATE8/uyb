package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.SitesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    @Getter
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final WebCrawler webCrawler;
    private final ExecutorService siteIndexingExecutor = Executors.newCachedThreadPool();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

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
            Document doc = webCrawler.fetchPageContent(site.getUrl());
            if (doc != null) {
                savePage(site, site.getUrl(), doc);
                updateSiteStatus(site, Status.INDEXED, null);
                logger.info("Индексация сайта завершена успешно: {}", site.getUrl());
            } else {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + site.getUrl());
                logger.error("Не удалось получить содержимое страницы: {}", site.getUrl());
            }
        } catch (Exception e) {
            String errorMessage = "Ошибка индексации сайта " + site.getUrl() + ": " + e.getMessage();
            updateSiteStatus(site, Status.FAILED, errorMessage);
            logger.error(errorMessage, e);
        }
    }

    private void savePage(Site site, String url, Document doc) {
        int statusCode = getStatusCode(); // Замените на вашу логику получения кода статуса
        if (!pageRepository.existsBySiteAndPath(site, url)) {
            Page page = new Page(site, url, statusCode, doc.html());
            pageRepository.save(page);
            logger.info("Сохранена страница: {} для сайта: {}", url, site.getUrl());
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
        indexingInProgress.set(false);
        logger.info("Запрос на остановку индексации поступил.");

        siteIndexingExecutor.shutdownNow();
        forkJoinPool.shutdownNow();

        siteRepository.findAll().forEach(site -> {
            if (!pageRepository.existsBySiteId(site.getId())) {
                updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            }
        });
    }

    private int getStatusCode() {
        // Реализуйте логику для получения кода статуса страницы
        return 200; // пример статуса
    }
}
