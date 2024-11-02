package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteBaza;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final SitesList sitesList;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Autowired
    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
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
            SiteBaza siteEntity = new SiteBaza(); // Создание экземпляра SiteBaza
            try {
                // Сохраняем информацию о сайте в базе данных
                siteEntity.setUrl(site.getUrl());
                siteEntity.setName(site.getName());
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity); // Сохраняем сайт

                Document document = Jsoup.connect(site.getUrl()).get();
                String title = document.title();
                String body = document.body().text();

                // Индексируем контент страницы
                indexContent(siteEntity, title, body);
            } catch (IOException e) {
                logger.error("Ошибка при извлечении контента с сайта: {}", site.getUrl(), e);
                // Обновляем статус сайта на FAILED в случае ошибки
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(e.getMessage());
                siteRepository.save(siteEntity); // Обновляем информацию о сайте
            } finally {
                long siteDuration = System.currentTimeMillis() - siteStartTime; // Calculate site duration
                logger.info("Индексация сайта {} завершена за {} мс", site.getUrl(), siteDuration);
            }
        }
        long totalDuration = System.currentTimeMillis() - startTime; // Total duration
        logger.info("Индексация завершена за {} мс.", totalDuration);
    }

    private void indexContent(SiteBaza site, String title, String body) {
        // Сохраняем информацию о странице в базе данных
        Page page = new Page();
        page.setSiteId(site.getId());
        page.setPath("/"); // Здесь установите правильный путь
        page.setCode(200); // Установите код ответа, например, 200
        page.setContent(body); // Содержимое страницы

        pageRepository.save(page); // Сохраняем страницу в базе данных

        // Реализуйте вашу логику индексации с использованием Apache Lucene
        logger.info("Индексация сайта: {}", site.getUrl());
        logger.info("Заголовок: {}", title);
        logger.info("Содержимое: {}", body.substring(0, Math.min(body.length(), 100)) + "...");
    }
}