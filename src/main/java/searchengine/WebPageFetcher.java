package searchengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.*;

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
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            int siteId = getOrCreateSiteId(conn, url);
            String path = getRelativePath(url);

            // Проверка, существует ли страница
            if (!isPageExist(conn, siteId, path)) {
                String insertPageSQL = "INSERT INTO page (site_id, path, content) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertPageSQL)) {
                    stmt.setInt(1, siteId);
                    stmt.setString(2, path);
                    stmt.setString(3, title + " " + description);
                    stmt.executeUpdate();
                    conn.commit();
                    logger.info("Страница успешно сохранена в базу данных: {}", url);
                } catch (SQLException e) {
                    conn.rollback();
                    logger.error("Ошибка при вставке данных в таблицу page. Транзакция откатывается.", e);
                }
            } else {
                logger.info("Страница уже существует в базе данных: {}", url);
            }

            int pageId = getPageId(conn, siteId, path);
            Map<String, Integer> lemmaFrequencyMap = extractLemmasAndFrequency(title + " " + description);
            for (Map.Entry<String, Integer> entry : lemmaFrequencyMap.entrySet()) {
                String lemma = entry.getKey();
                int frequency = entry.getValue();

                int lemmaId = getOrCreateLemmaId(conn, lemma);
                saveLemmaIndex(conn, pageId, lemmaId, frequency);
            }

        } catch (SQLException e) {
            logger.error("Ошибка при подключении к базе данных: ", e);
        }
    }

    // Метод для проверки существования страницы в базе данных
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

    // Метод для получения или создания ID сайта
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

    private static int getPageId(Connection conn, int siteId, String path) throws SQLException {
        String selectPageSQL = "SELECT id FROM page WHERE site_id = ? AND path = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectPageSQL)) {
            stmt.setInt(1, siteId);
            stmt.setString(2, path);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Страница не найдена в базе данных");
    }

    private static int getOrCreateLemmaId(Connection conn, String lemma) throws SQLException {
        String selectLemmaSQL = "SELECT id FROM lemma WHERE lemma = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectLemmaSQL)) {
            stmt.setString(1, lemma);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertLemmaSQL = "INSERT INTO lemma (lemma) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertLemmaSQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, lemma);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Не удалось создать запись для леммы.");
                }
            }
        }
    }

    private static void saveLemmaIndex(Connection conn, int pageId, int lemmaId, int frequency) throws SQLException {
        String selectIndexSQL = "SELECT id FROM `index` WHERE page_id = ? AND lemma_id = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectIndexSQL)) {
            selectStmt.setInt(1, pageId);
            selectStmt.setInt(2, lemmaId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    // Если запись существует, обновляем rank
                    int indexId = rs.getInt("id");
                    String updateRankSQL = "UPDATE `index` SET `rank` = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateRankSQL)) {
                        updateStmt.setInt(1, frequency);
                        updateStmt.setInt(2, indexId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Если записи нет, создаем новую
                    String insertIndexSQL = "INSERT INTO `index` (page_id, lemma_id, `rank`) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertIndexSQL)) {
                        insertStmt.setInt(1, pageId);
                        insertStmt.setInt(2, lemmaId);
                        insertStmt.setInt(3, frequency);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }


    private static Map<String, Integer> extractLemmasAndFrequency(String text) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        String[] words = text.split("\\W+");
        for (String word : words) {
            String lemma = word.toLowerCase();
            if (lemma.length() > 1) {  // Фильтруем короткие слова, например, односимвольные
                frequencyMap.put(lemma, frequencyMap.getOrDefault(lemma, 0) + 1);
            }
        }
        return frequencyMap;
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
