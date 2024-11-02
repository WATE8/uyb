package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private static final int MAX_DEPTH = 3; // Максимальная глубина индексации
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final SitesList sitesList;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Set<String> indexedUrls = new HashSet<>(); // Множество для отслеживания проиндексированных URL

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
        long startTime = System.currentTimeMillis(); // Начало измерения времени
        for (Site site : sitesList.getSites()) {
            long siteStartTime = System.currentTimeMillis(); // Время для каждого сайта
            SiteBaza siteEntity = new SiteBaza(); // Создание экземпляра SiteBaza
            try {
                // Сохраняем информацию о сайте в базе данных
                siteEntity.setUrl(site.getUrl());
                siteEntity.setName(site.getName());
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity); // Сохраняем сайт

                // Индексируем главную страницу сайта
                indexPageAndLinks(siteEntity, site.getUrl(), 0);

                // Устанавливаем статус сайта на INDEXED после успешной индексации
                siteEntity.setStatus(Status.INDEXED);
                siteRepository.save(siteEntity); // Обновляем информацию о сайте

            } catch (IOException e) {
                logger.error("Ошибка при извлечении контента с сайта: {}", site.getUrl(), e);
                // Обновляем статус сайта на FAILED в случае ошибки
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(e.getMessage());
                siteRepository.save(siteEntity); // Обновляем информацию о сайте
            } finally {
                long siteDuration = System.currentTimeMillis() - siteStartTime; // Время индексации сайта
                logger.info("Индексация сайта {} завершена за {} мс", site.getUrl(), siteDuration);
            }
        }
        long totalDuration = System.currentTimeMillis() - startTime; // Общее время индексации
        logger.info("Индексация завершена за {} мс.", totalDuration);
    }

    private void indexPageAndLinks(SiteBaza site, String url, int depth) throws IOException {
        if (depth > MAX_DEPTH || indexedUrls.contains(url)) {
            return; // Выход, если достигнута максимальная глубина или URL уже проиндексирован
        }

        indexedUrls.add(url); // Добавляем URL в список проиндексированных

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .execute();

            String contentType = response.contentType();
            if (contentType == null || (!contentType.startsWith("text/") && !contentType.startsWith("application/xml"))) {
                logger.warn("Некорректный тип контента для URL: {}", url);
                return; // Пропустить этот URL
            }

            Document document = response.parse();
            String title = document.title();
            String body = document.body().text();

            indexContent(site, title, body, url); // Индексируем содержимое страницы

            // Извлекаем все ссылки на странице
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String absUrl = link.absUrl("href");
                if (isValidUrl(absUrl, site.getUrl())) { // Проверка на допустимый URL
                    indexPageAndLinks(site, absUrl, depth + 1); // Рекурсивно индексируем найденные ссылки
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при извлечении контента с URL: {}", url, e);
        }
    }

    private boolean isValidUrl(String url, String baseUrl) {
        return url.startsWith(baseUrl); // Проверка, что URL начинается с базового URL сайта
    }

    private void indexContent(SiteBaza site, String title, String body, String url) {
        // Сохраняем информацию о странице в базе данных
        Page page = new Page();
        page.setSiteId(site.getId());
        page.setPath(url); // Установите правильный путь (можно извлечь из URL)
        page.setCode(200); // Установите код ответа, например, 200
        page.setContent(body); // Содержимое страницы

        pageRepository.save(page); // Сохраняем страницу в базе данных

        // Реализуйте вашу логику индексации с использованием Apache Lucene
        logger.info("Индексация сайта: {}", site.getUrl());
        logger.info("Заголовок: {}", title);
        logger.info("Содержимое: {}", body.substring(0, Math.min(body.length(), 100)) + "...");
    }
}
