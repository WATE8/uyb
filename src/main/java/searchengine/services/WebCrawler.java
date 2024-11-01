package searchengine.services;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Setter // Включает методы сеттера Lombok для полей класса
@Component // Регистрирует класс как Spring компонент
public class WebCrawler {

    private static final int MIN_DELAY = 500;
    private static final int MAX_DELAY = 5000;
    private static final int TIMEOUT = 10000; // Таймаут на 10 секунд
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private String referrer = "http://www.google.com";

    public Document fetchPageContent(String url) {
        try {
            // Случайная задержка для имитации человеческого поведения
            Thread.sleep(MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY)));

            return Jsoup.connect(url)
                    .userAgent(userAgent) // Использует поле user agent
                    .referrer(referrer) // Использует поле referrer
                    .timeout(TIMEOUT) // Устанавливает тайм-аут
                    .get();
        } catch (IOException e) {
            logger.error("Ошибка при получении содержимого страницы {}: {}", url, e.getMessage());
            return null; // Возвращает null при ошибке
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливает статус прерывания
            logger.error("Индексация была прервана: {}", e.getMessage());
            return null; // Возвращает null при прерывании
        }
    }

    public int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();
            return response.statusCode();
        } catch (IOException e) {
            logger.error("Ошибка при получении статуса для URL {}: {}", url, e.getMessage());
            return -1; // Возвращает -1, чтобы указать на ошибку
        }
    }
}
