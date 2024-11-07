package searchengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class WebPageProcessor {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/search_engine";
    private static final String USER = "root";
    private static final String PASSWORD = "asuzncmi666";
    private static final Logger logger = LoggerFactory.getLogger(WebPageProcessor.class);

    public static void main(String[] args) {
        String[] urls = {"https://volochek.life", "http://www.playback.ru"};

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            for (String url : urls) {
                try {
                    String html = getHtmlFromUrl(url);
                    int pageId = saveHtmlToDatabase(conn, url, html);
                    String text = extractTextFromHtml(html);
                    Map<String, Integer> wordCount = countWords(text);
                    int siteId = getSiteId(conn, url);
                    saveWordsToDatabase(conn, wordCount, pageId, siteId);
                } catch (SQLException e) {
                    logger.error("SQL exception occurred while processing URL: {}", url, e);
                } catch (Exception e) {
                    logger.error("Unexpected exception occurred while processing URL: {}", url, e);
                }
            }
        } catch (SQLException e) {
            logger.error("Connection failed: ", e);
        }
    }

    private static String getHtmlFromUrl(String url) throws Exception {
        try {
            Document document = Jsoup.connect(url).get();
            return document.html();
        } catch (Exception e) {
            logger.error("Error fetching HTML from URL: {}", url, e);
            throw e;
        }
    }

    private static int getSiteId(Connection conn, String url) throws SQLException {
        String siteUrl = extractBaseUrl(url);

        String selectSiteSql = "SELECT id FROM site WHERE url = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSiteSql)) {
            stmt.setString(1, siteUrl);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                String insertSiteSql = "INSERT INTO site (url, name, status) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSiteSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, siteUrl);
                    insertStmt.setString(2, siteUrl);
                    insertStmt.setInt(3, 1);
                    insertStmt.executeUpdate();

                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to insert site and retrieve site_id.");
                    }
                }
            }
        }
    }

    private static int saveHtmlToDatabase(Connection conn, String url, String html) throws SQLException {
        int siteId = getSiteId(conn, url);

        String insertPageSql = "INSERT INTO page (site_id, path, code, content) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertPageSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, siteId);
            stmt.setString(2, url);
            stmt.setInt(3, 200);
            stmt.setString(4, html);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Failed to retrieve page ID.");
            }
        }
    }

    private static void saveWordsToDatabase(Connection conn, Map<String, Integer> wordCount, int pageId, int siteId) throws SQLException {
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();

            String insertLemmaSql = "INSERT INTO lemma (lemma) VALUES (?) ON DUPLICATE KEY UPDATE lemma = lemma";
            try (PreparedStatement stmt = conn.prepareStatement(insertLemmaSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, word);
                stmt.executeUpdate();

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int lemmaId = generatedKeys.getInt(1);

                    String insertIndexSql = "INSERT INTO `index` (page_id, lemma_id, site_id, rank) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement stmtIndex = conn.prepareStatement(insertIndexSql)) {
                        stmtIndex.setInt(1, pageId);
                        stmtIndex.setInt(2, lemmaId);
                        stmtIndex.setInt(3, siteId);
                        stmtIndex.setFloat(4, (float) count);
                        stmtIndex.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                logger.error("Error while saving words to the database: ", e);
                throw e;
            }
        }
    }

    private static String extractTextFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        return doc.body().text();
    }

    private static Map<String, Integer> countWords(String text) {
        String[] words = text.split("\\s+");
        Map<String, Integer> wordCount = new HashMap<>();

        for (String word : words) {
            word = word.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (!word.isEmpty()) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        return wordCount;
    }

    private static String extractBaseUrl(String url) {
        return url.split("/")[2];
    }
}
