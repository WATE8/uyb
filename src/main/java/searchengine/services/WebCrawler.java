package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.io.IOException;

@Component
public class WebCrawler {

    private static final int MIN_DELAY = 500;
    private static final int MAX_DELAY = 5000;
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    @Autowired
    private SiteRepository siteRepository;

    public Document fetchPageContent(String url, Site site, Status status) throws IOException, InterruptedException {
        validateUrl(url);

        // Задержка для имитации поведения человека
        Thread.sleep(getRandomDelay());

        String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
        String referrer = "http://www.google.com";

        logger.info("Fetching content from URL: {}", url);
        Document document = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(10000)
                .get();

        updateSiteStatus(site, status);
        return document;
    }

    public int getStatusCode(String url) {
        try {
            validateUrl(url);
            logger.info("Checking status code for URL: {}", url);
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();
            return response.statusCode();
        } catch (IOException e) {
            logger.error("Error fetching status code for URL: {} - {}", url, e.getMessage());
            return -1; // Возвращает -1, чтобы указать на ошибку
        }
    }

    private void validateUrl(String url) {
        if (!isValidUrl(url)) {
            logger.warn("Malformed URL detected: {}", url);
            throw new IllegalArgumentException("Malformed URL: " + url);
        }
    }

    private int getRandomDelay() {
        return MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY));
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void updateSiteStatus(Site site, Status status) {
        if (site != null) {
            site.setStatus(status); // Убедитесь, что метод updateStatus есть в вашем классе Site
            site.setStatusTime(LocalDateTime.now()); // Обновление времени статуса
            siteRepository.save(site);
            logger.info("Updated site status: {} to {}", site.getUrl(), status);
        } else {
            logger.warn("Attempted to update status for a null site.");
        }
    }
}
