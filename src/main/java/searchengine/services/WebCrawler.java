package searchengine.services;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Setter // Enable Lombok's setter methods for the class fields
public class WebCrawler {

    private static final int MIN_DELAY = 500;
    private static final int MAX_DELAY = 5000;
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private String referrer = "http://www.google.com";

    public Document fetchPageContent(String url) throws IOException, InterruptedException {
        // Random delay to mimic human browsing
        Thread.sleep(MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY)));

        return Jsoup.connect(url)
                .userAgent(userAgent) // Use the user agent field
                .referrer(referrer) // Use the referrer field
                .timeout(10000) // Set timeout to 10 seconds
                .get();
    }

    public int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();
            return response.statusCode();
        } catch (IOException e) {
            logger.error("Error fetching status code for URL: {} - {}", url, e.getMessage());
            return -1; // Return -1 to indicate an error
        }
    }
}