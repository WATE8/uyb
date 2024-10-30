package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class WebCrawler {

    private static final int MIN_DELAY = 500; // Минимальная задержка в миллисекундах
    private static final int MAX_DELAY = 5000; // Максимальная задержка в миллисекундах

    public Document fetchPageContent(String url) {
        try {
            // Задержка перед отправкой запроса
            int delay = MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY));
            Thread.sleep(delay);

            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
        } catch (Exception e) {
            System.out.println("Ошибка при получении страницы: " + e.getMessage());
            return null;
        }
    }
}