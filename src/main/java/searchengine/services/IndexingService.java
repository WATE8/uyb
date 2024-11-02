package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.config.Site;
import searchengine.config.SitesList;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class); // Инициализация логгера
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final SitesList sitesList;

    @Autowired
    public IndexingService(SitesList sitesList) {
        this.sitesList = sitesList;
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

    public void startIndexing() {
        if (indexingInProgress.compareAndSet(false, true)) {
            try {
                performIndexing();
            } finally {
                indexingInProgress.set(false);
            }
        } else {
            throw new IllegalStateException("Индексация уже запущена");
        }
    }

    private void performIndexing() {
        long startTime = System.currentTimeMillis(); // Start time measurement
        for (Site site : sitesList.getSites()) {
            long siteStartTime = System.currentTimeMillis(); // Start time for each site
            try {
                Document document = Jsoup.connect(site.getUrl()).get();
                String title = document.title();
                String body = document.body().text();
                indexContent(site.getUrl(), title, body);
            } catch (IOException e) {
                logger.error("Ошибка при извлечении контента с сайта: {}", site.getUrl(), e);
                // Возможно, стоит добавить логику повторной попытки или сохранить состояние для дальнейшей обработки
            } finally {
                long siteDuration = System.currentTimeMillis() - siteStartTime; // Calculate site duration
                logger.info("Индексация сайта {} завершена за {} мс", site.getUrl(), siteDuration);
            }
        }
        long totalDuration = System.currentTimeMillis() - startTime; // Total duration
        logger.info("Индексация завершена за {} мс.", totalDuration);
    }

    private void indexContent(String url, String title, String body) {
        // Реализуйте вашу логику индексации с использованием Apache Lucene
        logger.info("Индексация сайта: {}", url);
        logger.info("Заголовок: {}", title);
        logger.info("Содержимое: {}", body.substring(0, Math.min(body.length(), 100)) + "...");
    }
}