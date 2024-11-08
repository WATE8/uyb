package searchengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;

public class WebPageFetcher {

    private static final Logger logger = LoggerFactory.getLogger(WebPageFetcher.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3306/search_engine"; // Используем MySQL
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "asuzncmi666";

    public static void extractTitleAndDescription(String url) {
        try {
            // Загружаем HTML-документ
            Document document = Jsoup.connect(url).get();

            // Получаем заголовок
            Element titleElement = document.select("title").first();
            // Получаем описание
            Element metaDescription = document.select("meta[name=description]").first();

            // Извлекаем текст заголовка и описания
            String title = titleElement != null ? titleElement.text() : "";
            String description = metaDescription != null ? metaDescription.attr("content") : "";

            // Логируем заголовок и описание
            if (titleElement != null) {
                logger.info("Заголовок страницы: {}", title);
            }
            if (metaDescription != null) {
                logger.info("Описание страницы: {}", description);
            }

            // Сохраняем данные в базу данных
            savePageToDatabase(url, title, description);

        } catch (IOException e) {
            logger.error("Ошибка при загрузке страницы: {}", url, e);
        }
    }

    private static void savePageToDatabase(String url, String title, String description) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false); // Начало транзакции

            int siteId = getOrCreateSiteId(conn, url);

            // SQL-запрос для вставки данных в таблицу `page`
            String insertPageSQL = "INSERT INTO page (site_id, path, content) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertPageSQL)) {
                stmt.setInt(1, siteId); // Идентификатор сайта
                stmt.setString(2, url); // URL страницы как path
                stmt.setString(3, title + " " + description); // title и description объединены как content
                stmt.executeUpdate();
                conn.commit(); // Подтверждаем транзакцию
                logger.info("Страница успешно сохранена в базу данных: {}", url);
            } catch (SQLException e) {
                conn.rollback(); // Откат транзакции в случае ошибки
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении страницы в базу данных: ", e);
        }
    }

    private static int getOrCreateSiteId(Connection conn, String url) throws SQLException {
        // Получаем домен из URL
        String domain = getDomainFromUrl(url);

        // Проверяем, существует ли сайт в таблице `site`
        String selectSiteSQL = "SELECT id FROM site WHERE domain = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSiteSQL)) {
            selectStmt.setString(1, domain);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id"); // Если сайт существует, возвращаем его ID
                }
            }
        }

        // Если сайт не существует, создаем его
        String insertSiteSQL = "INSERT INTO site (domain, name) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSiteSQL, Statement.RETURN_GENERATED_KEYS)) {
            String name = getDomainFromUrl(url); // Для простоты, используем домен как имя
            insertStmt.setString(1, domain);
            insertStmt.setString(2, name);
            insertStmt.executeUpdate();
            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Возвращаем сгенерированный ID
                } else {
                    throw new SQLException("Не удалось создать запись для нового сайта.");
                }
            }
        }
    }

    // Метод для извлечения домена из URL с обработкой исключения
    private static String getDomainFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (MalformedURLException e) {
            logger.error("Ошибка при разборе URL: {}", url, e);
            return ""; // Возвращаем пустую строку, если URL невалидный
        }
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
