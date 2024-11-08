package searchengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebPageFetcher {

    private static final Logger logger = LoggerFactory.getLogger(WebPageFetcher.class);

    public static void extractTitleAndDescription(String url) {
        try {
            // Загружаем HTML-документ
            Document document = Jsoup.connect(url).get();

            // Получаем заголовок
            Element titleElement = document.select("title").first();
            // Получаем описание
            Element metaDescription = document.select("meta[name=description]").first();

            // Логируем заголовок и описание
            if (titleElement != null) {
                logger.info("Заголовок страницы: {}", titleElement.text());
            }
            if (metaDescription != null) {
                logger.info("Описание страницы: {}", metaDescription.attr("content"));
            }
        } catch (IOException e) {
            logger.error("Ошибка при загрузке страницы: {}", url, e);
        }
    }

    public static void main(String[] args) {
        String[] urls = {
                "http://www.playback.ru",  // Первый URL
                "https://volochek.life"    // Второй URL
        };

        for (String url : urls) {
            logger.info("Получение информации для: {}", url);
            extractTitleAndDescription(url);
            logger.info("\n==============================\n");
        }
    }
}
