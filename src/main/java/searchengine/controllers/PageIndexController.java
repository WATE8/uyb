package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.services.PageIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PageIndexController {

    private static final Logger logger = LoggerFactory.getLogger(PageIndexController.class);

    private final PageIndexer pageIndexer;
    private final List<String> allowedDomains;

    // Конструктор для внедрения зависимостей
    public PageIndexController(PageIndexer pageIndexer, SitesList sitesList) {
        this.pageIndexer = pageIndexer;
        this.allowedDomains = sitesList.getAllowedDomains();  // Получаем список разрешённых доменов
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam("url") String url) {
        if (!isValidDomain(url)) {
            logger.warn("Доступ к странице с доменом {} запрещён", url);
            return new ResponseEntity<>(Map.of("result", false, "error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
        }

        try {
            // Индексация страницы
            logger.info("Начинаю индексацию страницы: {}", url);
            pageIndexer.indexPage(url);
            return new ResponseEntity<>(Map.of("result", true), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы: {}", url, e);
            return new ResponseEntity<>(Map.of("result", false, "error", "Произошла ошибка при индексации страницы"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Проверка, что домен страницы находится в разрешённом списке
    private boolean isValidDomain(String url) {
        try {
            String domain = new URL(url).getHost();
            boolean isValid = allowedDomains.contains(domain);
            if (!isValid) {
                logger.warn("Домен {} не разрешён для индексации", domain);
            }
            return isValid;
        } catch (Exception e) {
            logger.warn("Неверный URL или ошибка при проверке домена: {}", url, e);
            return false;
        }
    }
}
