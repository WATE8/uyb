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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private static final int MAX_DEPTH = 5; // Максимальная глубина индексации
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Set<String> indexedUrls = new HashSet<>();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

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
            stopRequested.set(false); // Сбрасываем флаг остановки
            try {
                performIndexing(depth);
            } finally {
                indexingInProgress.set(false);
            }
        } else {
            throw new IllegalStateException("Индексация уже запущена");
        }
    }

    public void stopIndexing() {
        if (indexingInProgress.get()) {
            stopRequested.set(true); // Устанавливаем флаг остановки
            // Обновляем статус для всех сайтов
            for (Site site : sitesList.getSites()) {
                SiteBaza siteEntity = siteRepository.findByUrl(site.getUrl());
                if (siteEntity != null) {
                    siteEntity.setStatus(Status.FAILED);
                    siteEntity.setLastError("Индексация остановлена пользователем");
                    siteRepository.save(siteEntity);
                }
            }
            logger.info("Индексация остановлена пользователем");
        } else {
            throw new IllegalStateException("Индексация не запущена");
        }
    }

    private void performIndexing(int depth) {
        long startTime = System.currentTimeMillis();
        for (Site site : sitesList.getSites()) {
            forkJoinPool.execute(() -> {
                long siteStartTime = System.currentTimeMillis();
                SiteBaza siteEntity = createSiteEntity(site);

                try {
                    forkJoinPool.invoke(new PageIndexer(siteEntity, site.getUrl(), 0, depth));
                    updateSiteStatus(siteEntity, Status.INDEXED, null);
                } catch (Exception e) {
                    logger.error("Ошибка при извлечении контента с сайта: {}", site.getUrl(), e);
                    updateSiteStatus(siteEntity, Status.FAILED, "Ошибка индексации: " + e.getMessage());
                } finally {
                    siteRepository.save(siteEntity);
                    logSiteIndexingDuration(site.getUrl(), siteStartTime);
                    try {
                        // Задержка перед следующим запросом
                        Thread.sleep(1000); // Задержка 1 секунда (можно настроить)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Восстановить прерывание
                        logger.warn("Задержка прервана");
                    }
                }
            });
        }
        logger.info("Индексация завершена за {} мс.", System.currentTimeMillis() - startTime);
    }


    private SiteBaza createSiteEntity(Site site) {
        SiteBaza siteEntity = new SiteBaza();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity); // Сохраняем начальный статус
        return siteEntity;
    }

    private void updateSiteStatus(SiteBaza siteEntity, Status status, String error) {
        siteEntity.setStatus(status);
        siteEntity.setLastError(error);
        siteRepository.save(siteEntity);
    }

    private void logSiteIndexingDuration(String url, long startTime) {
        long siteDuration = System.currentTimeMillis() - startTime;
        logger.info("Индексация сайта {} завершена за {} мс", url, siteDuration);
    }

    private class PageIndexer extends RecursiveTask<Void> {
        private final SiteBaza site;
        private final String url;
        private final int depth;
        private final int maxDepth;

        public PageIndexer(SiteBaza site, String url, int depth, int maxDepth) {
            this.site = site;
            this.url = url;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }

        @Override
        protected Void compute() {
            if (stopRequested.get() || depth >= maxDepth || indexedUrls.contains(url)) {
                return null; // Выход, если остановлено, достигнута максимальная глубина или URL уже проиндексирован
            }

            // Проверка на существование страницы по пути
            Page existingPage = pageRepository.findByPath(url); // Используем findByPath
            if (existingPage != null) {
                // Если страница уже проиндексирована, удаляем ее
                pageRepository.delete(existingPage);
                logger.info("Существующая индексация удалена для URL: {}", url);
            }

            indexedUrls.add(url);

            try {
                // Обновляем время статуса перед началом обработки страницы
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site); // Обновляем статус времени

                // Используем фейковый User-Agent и referrer
                Connection.Response response = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .execute();

                String contentType = response.contentType();
                if (contentType == null || (!contentType.startsWith("text/") && !contentType.startsWith("application/xml"))) {
                    logger.warn("Некорректный тип контента для URL: {}", url);
                    return null;
                }

                Document document = response.parse();
                String title = document.title();
                String body = document.body().text();

                // Индексация содержимого страницы с установкой статуса
                indexContent(site, title, body, url, 200); // Устанавливаем код состояния 200

                // Извлечение ссылок и создание новых задач для каждой ссылки
                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String absUrl = link.absUrl("href");
                    if (isValidUrl(absUrl, site.getUrl())) {
                        // Создаем новую задачу для каждой ссылки
                        PageIndexer subTask = new PageIndexer(site, absUrl, depth + 1, maxDepth);
                        subTask.fork(); // Запускаем задачу
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка при извлечении контента с URL: {}", url, e);
                indexContent(site, "Ошибка", "Ошибка при извлечении содержимого", url, 500); // Код ошибки
            }
            return null;
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
        logger.info("Содержимое: {}", body.substring(0, Math.min(body.length(), 100)) + "..."); // Логирование первых 100 символов содержимого
    }
}
