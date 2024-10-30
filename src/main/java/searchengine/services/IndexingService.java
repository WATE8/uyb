package searchengine.services;

import lombok.Getter;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException; // Import for IOException
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
    private final ExecutorService pageCrawlingExecutor = Executors.newCachedThreadPool(); // New Executor for page crawling

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

        // Запуск обработки сайтов в отдельных потоках
        List<Site> sites = siteRepository.findAll();
        sites.forEach(site -> siteIndexingExecutor.submit(() -> indexSite(site)));
    }

    private void indexSite(Site site) {
        if (stopIndexing.get()) {
            updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
            return;
        }

        updateSiteStatus(site, Status.INDEXING, null);
        pageRepository.deleteBySiteId(site.getId());

        try {
            Document doc = webCrawler.fetchPageContent(site.getUrl());
            if (doc != null) {
                // Submit the PageCrawlerTask to the pageCrawlingExecutor
                pageCrawlingExecutor.submit(new PageCrawlerTask(site, site.getUrl()));
                updateSiteStatus(site, Status.INDEXED, null);
            } else {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + site.getUrl());
            }
        } catch (IOException e) { // Catching IOException
            updateSiteStatus(site, Status.FAILED, "Ошибка при загрузке страницы: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            updateSiteStatus(site, Status.FAILED, "Индексация прервана: " + e.getMessage());
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

    private class PageCrawlerTask implements Runnable { // Change to Runnable
        private final Site site;
        private final String url;

        public PageCrawlerTask(Site site, String url) {
            this.site = site;
            this.url = url;
        }

        @Override
        public void run() { // Implement run method
            if (stopIndexing.get() || pageRepository.existsBySiteAndPath(site, url)) {
                return;
            }

            try {
                Document doc = webCrawler.fetchPageContent(url);
                int statusCode = getStatusCode(url);
                Page page = new Page(); // Ensure this matches your Page constructor
                page.setSite(site);
                page.setPath(url);
                page.setCode(statusCode);
                page.setContent(doc.html());
                pageRepository.save(page);

                // Create tasks for new links
                List<String> links = doc.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .distinct()
                        .toList();
                for (String linkUrl : links) {
                    pageCrawlingExecutor.submit(new PageCrawlerTask(site, linkUrl)); // Submit new links
                }
            } catch (Exception e) {
                updateSiteStatus(site, Status.FAILED, e.getMessage());
            }
        }
    }

    private int getStatusCode(String url) {
        return webCrawler.getStatusCode(url);
    }
}
