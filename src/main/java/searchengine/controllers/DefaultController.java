package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Status;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class DefaultController {

    private final IndexingService indexingService;
    private final SiteRepository siteRepository;

    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (indexingService.isIndexingInProgress()) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Получаем список сайтов из базы данных
            List<Site> sites = siteRepository.findAll();
            if (sites.isEmpty()) {
                response.put("result", false);
                response.put("error", "Нет сайтов для индексации");
                return ResponseEntity.badRequest().body(response);
            }

            // Обновляем статус каждого сайта и запускаем индексацию
            for (Site site : sites) {
                site.setStatus(Status.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
                logger.info("Старт индексации сайта: {}", site.getUrl());
            }

            indexingService.startFullIndexing();
            response.put("result", true);
        } catch (Exception e) {
            logger.error("Ошибка во время процесса индексации: {}", e.getMessage());
            response.put("result", false);
            response.put("error", "Ошибка при запуске индексации");
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (!indexingService.isIndexingInProgress()) {
            response.put("result", false);
            response.put("error", "Индексация не запущена");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            indexingService.stopIndexing();
            logger.info("Индексация остановлена");
            response.put("result", true);
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации: {}", e.getMessage());
            response.put("result", false);
            response.put("error", "Ошибка при остановке индексации");
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}