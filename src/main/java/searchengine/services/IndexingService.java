package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
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
import java.util.List;
import java.util.Optional;
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
        if (indexingInProgress.get()) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        indexingInProgress.set(true);
        stopIndexing.set(false);

        sitesList.getSites().forEach(siteConfig -> {
            // Удаление существующего сайта и страниц
            Optional<Site> existingSiteOpt = siteRepository.findByUrl(siteConfig.getUrl());

            // Проверяем, существует ли сайт
            if (existingSiteOpt.isPresent()) {
                Site existingSite = existingSiteOpt.get(); // Получаем Site из Optional
                pageRepository.deleteBySiteId(existingSite.getId());
                siteRepository.deleteById(existingSite.getId());
            }

            // Создание нового сайта
            Site site = new Site();
            site.setUrl(siteConfig.getUrl());
            site.setName(siteConfig.getName());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            logger.info("Запущена индексация сайта: {}", site.getUrl());

            // Запуск индексации в отдельном потоке
            siteIndexingExecutor.submit(() -> indexSite(site));
        });
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
                forkJoinPool.invoke(new PageCrawlerTask(site, site.getUrl()));
            } else {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + site.getUrl());
            }
            updateSiteStatus(site, Status.INDEXED, null);
        } catch (Exception e) {
            updateSiteStatus(site, Status.FAILED, e.getMessage());
            logger.error("Ошибка индексации сайта {}: {}", site.getUrl(), e.getMessage());
        }
    }

    private void savePage(Site site, String url, Document doc) {
        int statusCode = getStatusCode(url);
        if (!pageRepository.existsBySiteAndPath(site, url)) {
            Page page = new Page(site, url, statusCode, doc.html());
            pageRepository.save(page);
            logger.info("Сохранена страница: {} для сайта: {}", url, site.getUrl());
        }
    }

    private void updateSiteStatus(Site site, Status status, String lastError) {
        site.updateStatus(status);  // Использование метода updateStatus
        if (lastError != null) {
            site.setLastError(lastError);
        }
        siteRepository.save(site);
        logger.info("Обновлен статус сайта: {} на {}", site.getUrl(), status);
    }

    public void stopIndexing() {
        stopIndexing.set(true);
        indexingInProgress.set(false);
        logger.info("Запрос на остановку индексации поступил.");

        siteIndexingExecutor.shutdownNow();
        forkJoinPool.shutdownNow();

        List<Site> sites = siteRepository.findAll();
        sites.forEach(site -> {
            if (!pageRepository.existsBySiteId(site.getId())) {
                updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        siteIndexingExecutor.shutdownNow();
        forkJoinPool.shutdownNow();
        logger.info("Попытка завершить потоки индексации...");

        try {
            if (!siteIndexingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.error("Executor service не завершился!");
            }
            if (!forkJoinPool.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.error("ForkJoinPool не завершился!");
            }
        } catch (InterruptedException e) {
            logger.error("Ошибка при завершении: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
        }
    }

    private int getStatusCode(String url) {
        return webCrawler.getStatusCode(url);
    }

    private class PageCrawlerTask extends RecursiveTask<Void> {
        private final Site site;
        private final String url;

        public PageCrawlerTask(Site site, String url) {
            this.site = site;
            this.url = url;
        }

        @Override
        protected Void compute() {
            if (stopIndexing.get() || pageRepository.existsBySiteAndPath(site, url)) {
                return null;
            }

            try {
                Thread.sleep(500 + (int) (Math.random() * 4500)); // Задержка между запросами 0,5–5 секунд

                Document doc = webCrawler.fetchPageContent(url); // Используем url
                if (doc != null) {
                    savePage(site, url, doc); // Используем url

                    List<PageCrawlerTask> subTasks = doc.select("a[href]").stream()
                            .map(link -> link.absUrl("href"))
                            .filter(linkUrl -> linkUrl.startsWith(site.getUrl()))
                            .distinct()
                            .map(linkUrl -> new PageCrawlerTask(site, linkUrl))
                            .toList();
                    invokeAll(subTasks);
                }
            } catch (Exception e) {
                updateSiteStatus(site, Status.FAILED, e.getMessage());
                logger.error("Ошибка при обработке страницы {}: {}", url, e.getMessage()); // Используем url
            }
            return null;
        }
    }
}