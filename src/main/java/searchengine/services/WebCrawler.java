package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class WebCrawler {

    private static final int MIN_DELAY = 500;
    private static final int MAX_DELAY = 5000;

    public Document fetchPageContent(String url) throws IOException, InterruptedException {
        // Random delay to mimic human browsing
        Thread.sleep(MIN_DELAY + (int) (Math.random() * (MAX_DELAY - MIN_DELAY)));
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();
    }

    public int getStatusCode(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();
            return response.statusCode();
        } catch (IOException e) {
            // Log the exception or handle it as needed
            System.err.println("Error fetching status code for URL: " + url + " - " + e.getMessage());
            return 0; // Return 0 for failure
        }
    }
}