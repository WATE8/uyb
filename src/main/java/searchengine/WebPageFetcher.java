package searchengine;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class WebPageFetcher {

    private static final Logger logger = LoggerFactory.getLogger(WebPageFetcher.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3306/search_engine";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "asuzncmi666";

    public static void extractTitleAndDescription(String url) {
        if (!isValidUrl(url)) {
            logger.error("Невалидный URL: {}", url);
            return;
        }

        try {
            Document document = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            String title = document.title();
            String description = document.select("meta[name=description]").attr("content");

            if (title.isEmpty()) {
                logger.warn("Заголовок отсутствует на странице: {}", url);
            }
            if (description.isEmpty()) {
                logger.warn("Описание отсутствует на странице: {}", url);
            }

            logger.info("Заголовок страницы: {}", title);
            logger.info("Описание страницы: {}", description);

            savePageToDatabase(url, title, description);

        } catch (IOException e) {
            logger.error("Ошибка при загрузке страницы: {}", url, e);
        }
    }

    private static void savePageToDatabase(String url, String title, String description) {
        String content = title + " " + description;
        Map<String, Integer> lemmas = lemmatize(content);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            int siteId = getOrCreateSiteId(conn, url);
            String path = getRelativePath(url);

            if (!isPageExist(conn, siteId, path)) {
                String insertPageSQL = "INSERT INTO page (site_id, path, content) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertPageSQL)) {
                    stmt.setInt(1, siteId);
                    stmt.setString(2, path);
                    stmt.setString(3, content);
                    stmt.executeUpdate();
                }

                saveLemmasToDatabase(conn, siteId, path, lemmas);
                conn.commit();
                logger.info("Страница и леммы успешно сохранены в базу данных: {}", url);
            } else {
                logger.info("Страница уже существует в базе данных: {}", url);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении данных в базу", e);
        }
    }

    private static Map<String, Integer> lemmatize(String text) {
        Map<String, Integer> lemmaCountMap = new HashMap<>();

        try (RussianAnalyzer analyzer = new RussianAnalyzer()) {
            TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String lemma = tokenStream.getAttribute(CharTermAttribute.class).toString();
                lemmaCountMap.put(lemma, lemmaCountMap.getOrDefault(lemma, 0) + 1);
            }
            tokenStream.close();
        } catch (IOException e) {
            logger.error("Ошибка при лемматизации текста: ", e);
        }
        return lemmaCountMap;
    }

    private static void saveLemmasToDatabase(Connection conn, int siteId, String path, Map<String, Integer> lemmas) {
        String insertLemmaSQL = "INSERT INTO lemma (site_id, path, lemma, frequency) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertLemmaSQL)) {
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                stmt.setInt(1, siteId);
                stmt.setString(2, path);
                stmt.setString(3, entry.getKey());
                stmt.setInt(4, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении лемм в базу данных", e);
        }
    }

    private static boolean isPageExist(Connection conn, int siteId, String path) throws SQLException {
        String selectPageSQL = "SELECT 1 FROM page WHERE site_id = ? AND path = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectPageSQL)) {
            stmt.setInt(1, siteId);
            stmt.setString(2, path);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int getOrCreateSiteId(Connection conn, String url) throws SQLException {
        String domain = getDomainFromUrl(url);

        if (domain == null || domain.isEmpty()) {
            throw new SQLException("Невозможно получить домен из URL: " + url);
        }

        String selectSiteSQL = "SELECT id FROM site WHERE domain = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSiteSQL)) {
            selectStmt.setString(1, domain);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSiteSQL = "INSERT INTO site (domain, name) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSiteSQL, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, domain);
            insertStmt.setString(2, domain);
            insertStmt.executeUpdate();
            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Не удалось создать запись для нового сайта.");
                }
            }
        }
    }

    private static String getDomainFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (MalformedURLException e) {
            logger.error("Ошибка при разборе URL: {}", url, e);
            return null;
        }
    }

    private static String getRelativePath(String url) {
        String domain = getDomainFromUrl(url);
        String path = url.replaceFirst("^(https?://)?(www\\.)?" + domain, "");
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static boolean isValidUrl(String url) {
        String regex = "^(https?://)?[\\w.-]+(\\.[a-z]{2,})+.*$";
        return url.matches(regex);
    }

    public static void main(String[] args) {
        String[] urls = {
                "http://www.playback.ru",
                "https://volochek.life"
        };

        for (String url : urls) {
            logger.info("Получение информации для: {}", url);
            extractTitleAndDescription(url);
            logger.info("\n==============================\n");
        }
    }
}
