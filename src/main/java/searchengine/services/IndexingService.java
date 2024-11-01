package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.SitesList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    @Getter
    private boolean indexingInProgress = false;
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final WebCrawler webCrawler;
    private final ExecutorService siteIndexingExecutor = Executors.newCachedThreadPool();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public void startFullIndexing() {
        if (indexingInProgress) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        indexingInProgress = true;
        stopIndexing.set(false);

        sitesList.getSites().forEach(siteConfig -> {
            Site site = new Site();
            site.setUrl(siteConfig.getUrl());
            site.setName(siteConfig.getName());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            siteIndexingExecutor.submit(() -> indexSite(site));
        });
    }

    private void indexSite(Site site) {
        if (stopIndexing.get()) {
            updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            return;
        }

        updateSiteStatus(site, Status.INDEXING, null);

        try {
            // Передаем статус при получении содержимого страницы
            Document doc = webCrawler.fetchPageContent(site.getUrl(), site, Status.INDEXING);
            if (doc != null) {
                pageRepository.deleteBySiteId(site.getId());
                processDocument(site, doc); // Передаем документ для обработки
            } else {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + site.getUrl());
            }
        } catch (Exception e) {
            logger.error("Ошибка индексации сайта: {}", site.getUrl(), e);
            updateSiteStatus(site, Status.FAILED, "Ошибка при индексации: " + e.getMessage());
        }
    }

    private void processDocument(Site site, Document doc) {
        updateSiteStatus(site, Status.INDEXED, null);
        forkJoinPool.invoke(new PageCrawlerTask(site, site.getUrl(), doc));
    }

    private void updateSiteStatus(Site site, Status status, String lastError) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        if (lastError != null) {
            site.setLastError(lastError);
        }
        siteRepository.save(site);
    }

    public void stopIndexing() {
        stopIndexing.set(true);
        indexingInProgress = false;

        List<Site> sites = siteRepository.findAll();
        sites.forEach(site -> {
            if (!pageRepository.existsBySiteId(site.getId())) {
                updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            }
        });
    }

    private class PageCrawlerTask extends RecursiveTask<Void> {
        private final Site site;
        private final String url;
        private final Document doc;

        public PageCrawlerTask(Site site, String url, Document doc) {
            this.site = site;
            this.url = url;
            this.doc = doc;
        }

        @Override
        protected Void compute() {
            if (stopIndexing.get() || pageRepository.existsBySiteAndPath(site, url)) {
                return null;
            }

            if (!isValidUrl(url)) {
                updateSiteStatus(site, Status.FAILED, "Некорректный URL: " + url);
                return null;
            }

            try {
                int statusCode = getStatusCode(url);
                if (!pageRepository.existsBySiteAndPath(site, url)) {
                    Page page = new Page(site, url, statusCode, doc.html());
                    pageRepository.save(page);
                }

                List<PageCrawlerTask> subTasks = doc.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .filter(linkUrl -> linkUrl.startsWith(site.getUrl()))
                        .distinct()
                        .map(linkUrl -> new PageCrawlerTask(site, linkUrl, doc))
                        .toList();
                invokeAll(subTasks);
            } catch (Exception e) {
                logger.error("Ошибка при индексации страницы: {}", url, e);
                updateSiteStatus(site, Status.FAILED, "Ошибка при индексации: " + e.getMessage());
            }
            return null;
        }
    }

    private int getStatusCode(String url) {
        return webCrawler.getStatusCode(url);
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"));
    }

    @PreDestroy
    public void shutdown() {
        stopIndexing.set(true); // Устанавливаем флаг перед завершением
        siteIndexingExecutor.shutdown();
        try {
            if (!siteIndexingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                siteIndexingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            siteIndexingExecutor.shutdownNow();
        }
        forkJoinPool.shutdown();
    }
}
