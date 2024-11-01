package searchengine.services;

import lombok.Getter;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy; // Import for @PreDestroy annotation
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
public class IndexingService {

    @Getter
    private boolean indexingInProgress = false;
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final WebCrawler webCrawler;
    private final ExecutorService siteIndexingExecutor = Executors.newCachedThreadPool();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(); // Create ForkJoinPool once

    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.webCrawler = new WebCrawler();
    }

    public void startFullIndexing() {
        if (indexingInProgress) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        indexingInProgress = true;
        stopIndexing.set(false);

        List<Site> sites = siteRepository.findAll();
        sites.forEach(site -> siteIndexingExecutor.submit(() -> indexSite(site)));
    }

    private void indexSite(Site site) {
        if (stopIndexing.get()) {
            updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            return;
        }

        updateSiteStatus(site, Status.INDEXING, null);
        pageRepository.deleteBySiteId(site.getId()); // Remove existing pages

        try {
            Document doc = webCrawler.fetchPageContent(site.getUrl());
            if (doc != null) {
                // Update site status and time
                site.setStatus(Status.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                // Start page indexing
                forkJoinPool.invoke(new PageCrawlerTask(site, site.getUrl()));
                updateSiteStatus(site, Status.INDEXED, null);
            } else {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + site.getUrl());
            }
        } catch (Exception e) {
            updateSiteStatus(site, Status.FAILED, e.getMessage());
        }
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

        public PageCrawlerTask(Site site, String url) {
            this.site = site;
            this.url = url;
        }

        @Override
        protected Void compute() {
            if (stopIndexing.get() || pageRepository.existsBySiteAndPath(site, url)) {
                return null; // Exit if indexing is stopped or page already exists
            }

            try {
                Document doc = webCrawler.fetchPageContent(url);
                int statusCode = getStatusCode(url);
                Page page = new Page(site, url, statusCode, doc.html());
                pageRepository.save(page); // Save page

                // Create tasks for new links
                List<PageCrawlerTask> subTasks = doc.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .distinct()
                        .map(linkUrl -> new PageCrawlerTask(site, linkUrl))
                        .toList();
                invokeAll(subTasks); // Recursively index new pages
            } catch (Exception e) {
                updateSiteStatus(site, Status.FAILED, e.getMessage());
            }
            return null;
        }
    }

    private int getStatusCode(String url) {
        return webCrawler.getStatusCode(url); // Get page status code
    }

    @PreDestroy // Annotation to run method before bean destruction
    public void shutdown() {
        siteIndexingExecutor.shutdownNow(); // Shutdown all indexing threads
        forkJoinPool.shutdown(); // Shutdown ForkJoinPool
    }
}