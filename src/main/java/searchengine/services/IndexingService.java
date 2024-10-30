package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingService {

    @Getter
    private boolean indexingInProgress = false;
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false);

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Value("${crawler.userAgent}")
    private String userAgent;

    @Value("${crawler.referrer}")
    private String referrer;

    @Value("${crawler.delay}")
    private long delay;

    public void startFullIndexing() {
        if (indexingInProgress) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        indexingInProgress = true;
        stopIndexing.set(false);

        new Thread(this::performIndexing).start();
    }

    private void performIndexing() {
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            if (stopIndexing.get()) {
                updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем");
                continue;
            }

            updateSiteStatus(site, Status.INDEXING, null);
            pageRepository.deleteBySiteId(site.getId());

            try {
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(new PageCrawlerTask(site, site.getUrl(), Set.of(site.getUrl())));
                updateSiteStatus(site, Status.INDEXED, null);
            } catch (Exception e) {
                updateSiteStatus(site, Status.FAILED, "Ошибка при индексации: " + e.getMessage());
            }
        }

        indexingInProgress = false;
    }

    public void stopIndexing() {
        stopIndexing.set(true);
    }

    private void updateSiteStatus(Site site, Status status, String error) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(error);
        siteRepository.save(site);
    }

    private class PageCrawlerTask extends RecursiveAction {
        private final Site site;
        private final String url;
        private final Set<String> visitedUrls;

        PageCrawlerTask(Site site, String url, Set<String> visitedUrls) {
            this.site = site;
            this.url = url;
            this.visitedUrls = visitedUrls;
        }

        @Override
        protected void compute() {
            if (stopIndexing.get() || visitedUrls.contains(url)) return;
            visitedUrls.add(url);

            try {
                Document document = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .referrer(referrer)
                        .get();

                Page page = new Page();
                page.setSite(site);
                page.setPath(url);
                page.setCode(document.connection().response().statusCode());
                page.setContent(document.html());
                pageRepository.save(page);

                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                List<PageCrawlerTask> subtasks = document.select("a[href]")
                        .stream()
                        .map(link -> link.absUrl("href"))
                        .filter(link -> !visitedUrls.contains(link))
                        .map(link -> new PageCrawlerTask(site, link, visitedUrls))
                        .toList();

                Thread.sleep(delay);
                invokeAll(subtasks);

            } catch (IOException | InterruptedException e) {
                updateSiteStatus(site, Status.FAILED, "Не удалось получить содержимое страницы: " + url);
            }
        }
    }
}