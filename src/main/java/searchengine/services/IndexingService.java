package searchengine.services;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status; // Убедитесь, что у вас есть этот импорт
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    @Getter
    private boolean indexingInProgress = false;
    private final AtomicBoolean stopIndexing = new AtomicBoolean(false); // Флаг остановки индексации

    private final SiteRepository siteRepository; // Репозиторий для таблицы Site
    private final PageRepository pageRepository; // Репозиторий для таблицы Page
    private final WebCrawler webCrawler; // Новый экземпляр WebCrawler

    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.webCrawler = new WebCrawler(); // Инициализация WebCrawler
    }

    public void startFullIndexing() {
        if (indexingInProgress) {
            throw new IllegalStateException("Индексация уже запущена");
        }

        indexingInProgress = true;
        stopIndexing.set(false);

        // Используем стандартный поток
        new Thread(this::performIndexing).start();
    }

    private void performIndexing() {
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            if (stopIndexing.get()) {
                throw new RuntimeException("Индексация остановлена пользователем");
            }

            // Устанавливаем статус на INDEXING
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            // Удаляем предыдущие страницы
            pageRepository.deleteBySiteId(site.getId());

            try {
                // Получаем содержимое главной страницы
                Document doc = webCrawler.fetchPageContent(site.getUrl());

                // Проверяем, получено ли содержимое
                if (doc != null) {
                    // Логика обхода страниц и сохранения их в БД
                    Page page = new Page();
                    page.setSite(site);
                    page.setPath(site.getUrl()); // Замените на правильный путь

                    // Получаем код статуса страницы
                    int statusCode = getStatusCode(site.getUrl());
                    page.setCode(statusCode); // Устанавливаем полученный код
                    page.setContent(doc.html()); // Содержимое страницы
                    pageRepository.save(page);

                    // Здесь добавьте логику для обхода всех найденных ссылок
                } else {
                    // Обработка ошибки получения содержимого
                    handleError(new RuntimeException("Не удалось получить содержимое страницы: " + site.getUrl()));
                }
            } catch (Exception e) {
                // Обработка ошибок, связанных с получением или обработкой
                handleError(e);
            }

            // Устанавливаем статус на INDEXED
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
        indexingInProgress = false; // Обновляем статус после завершения индексации
    }

    public void stopIndexing() {
        stopIndexing.set(true);
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            if (!pageRepository.existsBySiteId(site.getId())) { // Проверяем, если страницы еще не были добавлены
                site.setStatus(Status.FAILED); // Устанавливаем статус в FAILED
                site.setLastError("Индексация остановлена пользователем"); // Устанавливаем текст ошибки
                site.setStatusTime(LocalDateTime.now()); // Обновляем время статуса
                siteRepository.save(site); // Сохраняем изменения в базе данных
            }
        }
    }

    private void handleError(Exception e) {
        stopIndexing();
        System.out.println("Ошибка индексации: " + e.getMessage());
    }

    // Метод для получения кода статуса страницы
    private int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.HEAD) // Или Connection.Method.GET, если нужно
                    .execute();
            return response.statusCode();
        } catch (IOException e) {
            return 0; // Или другой код ошибки
        }
    }
}