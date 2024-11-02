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
    private static final int MAX_DEPTH = 5; // Максимальная глубина индексации
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Set<String> indexedUrls = new HashSet<>();

    @Autowired
    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

    public void startIndexing(int depth) {
        if (depth < 1 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Глубина индексации должна быть от 1 до " + MAX_DEPTH);
        }
        if (indexingInProgress.compareAndSet(false, true)) {
            try {
                performIndexing(depth);
            } finally {
                indexingInProgress.set(false);
            }
        } else {
            throw new IllegalStateException("Индексация уже запущена");
        }
    }

    private void performIndexing(int depth) {
        long startTime = System.currentTimeMillis();
        for (Site site : sitesList.getSites()) {
            long siteStartTime = System.currentTimeMillis();
            SiteBaza siteEntity = new SiteBaza();
            try {
                // Создаем новую запись в Site с статусом INDEXING
                siteEntity.setUrl(site.getUrl());
                siteEntity.setName(site.getName());
                siteEntity.setStatus(Status.INDEXING);  // Устанавливаем статус INDEXING
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);

                // Запуск индексации страницы с учетом глубины
                indexPageAndLinks(siteEntity, site.getUrl(), 0, depth);

                // Обновляем статус на INDEXED после завершения индексации
                siteEntity.setStatus(Status.INDEXED);
                siteRepository.save(siteEntity);
            } catch (IOException e) {
                // Обработка ошибок и обновление статуса на FAILED
                logger.error("Ошибка при извлечении контента с сайта: {}", site.getUrl(), e);
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(e.getMessage());
                siteRepository.save(siteEntity);
            } finally {
                long siteDuration = System.currentTimeMillis() - siteStartTime;
                logger.info("Индексация сайта {} завершена за {} мс", site.getUrl(), siteDuration);
            }
        }
        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("Индексация завершена за {} мс.", totalDuration);
    }

    private void indexPageAndLinks(SiteBaza site, String url, int depth, int maxDepth) throws IOException {
        if (depth >= maxDepth || indexedUrls.contains(url)) {
            return; // Выход, если достигнута максимальная глубина или URL уже проиндексирован
        }

        indexedUrls.add(url);

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .execute();

            String contentType = response.contentType();
            if (contentType == null || (!contentType.startsWith("text/") && !contentType.startsWith("application/xml"))) {
                logger.warn("Некорректный тип контента для URL: {}", url);
                return;
            }

            Document document = response.parse();
            String title = document.title();
            String body = document.body().text();

            // Индексация содержимого страницы с установкой статуса
            indexContent(site, title, body, url, 200); // Устанавливаем код состояния 200

            // Извлечение ссылок и рекурсивный обход
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String absUrl = link.absUrl("href");
                if (isValidUrl(absUrl, site.getUrl())) {
                    indexPageAndLinks(site, absUrl, depth + 1, maxDepth); // Рекурсивно индексируем
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при извлечении контента с URL: {}", url, e);
            // Дополнительно можно сохранить страницу с ошибкой, если это необходимо
            indexContent(site, "Ошибка", "Ошибка при извлечении содержимого", url, 500); // Код ошибки
        }
    }

    private boolean isValidUrl(String url, String baseUrl) {
        return url.startsWith(baseUrl);
    }

    private void indexContent(SiteBaza site, String title, String body, String url, int code) {
        Page page = new Page();
        page.setSiteId(site.getId());
        page.setPath(url);
        page.setCode(code); // Устанавливаем код состояния
        page.setContent(body);

        pageRepository.save(page); // Сохранение страницы в базе данных
        logger.info("Индексация страницы: {}", url);
        logger.info("Заголовок: {}", title);
        logger.info("Содержимое: {}", body.substring(0, Math.min(body.length(), 100)) + "...");
    }
}
