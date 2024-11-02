package searchengine.services;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Setter
@Component
public class WebCrawler {

    private static final int MIN_DELAY = 500;
    private static final int MAX_DELAY = 5000;
    private static final int TIMEOUT = 10000;
    private static final int ERROR_STATUS_CODE = -1;
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    @Value("${crawler.user-agent:Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6}")
    private String userAgent;

    @Value("${crawler.referrer:http://www.google.com}")
    private String referrer;

    public Document fetchPageContent(String url) throws IOException, InterruptedException {
        applyRandomDelay();
        try {
            return Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(TIMEOUT)
                    .get();
        } catch (IOException e) {
            logger.error("Ошибка при получении содержимого страницы {}: {}", url, e.getMessage());
            throw e;
        }
    }

    public int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.HEAD)
                    .timeout(TIMEOUT)
                    .execute();
            return response.statusCode();
        } catch (IOException e) {
            logger.error("Ошибка при получении статуса для URL {}: {}", url, e.getMessage());
            return ERROR_STATUS_CODE;
        }
    }

    private void applyRandomDelay() throws InterruptedException {
        Thread.sleep(MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY)));
    }
}