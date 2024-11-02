package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;
import searchengine.repositories.SiteRepository;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class DefaultController {

    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    // Удалены неиспользуемые pageRepository и sitesList

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultController.class);

    @PostMapping("/addSite")
    @Transactional
    public ResponseEntity<Map<String, Object>> addSite(@RequestBody searchengine.config.Site newSite) {
        Map<String, Object> responseMap = new HashMap<>();

        if (siteRepository.existsByUrl(newSite.getUrl())) {
            return createErrorResponse("Сайт уже существует", responseMap);
        }

        try {
            searchengine.model.Site modelSite = createModelSite(newSite);
            siteRepository.save(modelSite);
            logger.info("Сайт успешно добавлен: {}", modelSite.getUrl());
            return createSuccessResponse(responseMap, "Сайт успешно добавлен: " + modelSite.getUrl());
        } catch (Exception e) {
            logger.error("Ошибка при добавлении сайта: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка при добавлении сайта: " + e.getMessage(), responseMap);
        }
    }

    @GetMapping("/startIndexing")
    @Transactional
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> responseMap = new HashMap<>();

        try {
            if (indexingService.isIndexingInProgress()) {
                return createErrorResponse("Индексация уже запущена", responseMap);
            }

            indexingService.startFullIndexing();
            return createSuccessResponse(responseMap, "Индексация начата успешно");

        } catch (Exception e) {
            logger.error("Ошибка во время запуска индексации: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка запуска индексации: " + e.getMessage(), responseMap);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String, Object> responseMap = new HashMap<>();

        if (!indexingService.isIndexingInProgress()) {
            return createErrorResponse("Индексация не запущена", responseMap);
        }

        indexingService.stopIndexing();
        logger.info("Индексация остановлена пользователем.");
        return createSuccessResponse(responseMap, "Индексация остановлена успешно");
    }

    private searchengine.model.Site createModelSite(searchengine.config.Site newSite) {
        return new searchengine.model.Site(newSite.getUrl(), newSite.getName(), Status.INDEXING, LocalDateTime.now());
    }

    private ResponseEntity<Map<String, Object>> createSuccessResponse(Map<String, Object> responseMap, String message) {
        responseMap.put("status", "success");
        responseMap.put("message", message);
        return ResponseEntity.ok(responseMap);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, Map<String, Object> responseMap) {
        responseMap.put("status", "error");
        responseMap.put("message", message);
        return ResponseEntity.badRequest().body(responseMap);
    }
}
