package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;
import searchengine.repositories.SiteRepository;
import searchengine.repositories.PageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Status;
import searchengine.config.SitesList;

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
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

    @PostMapping("/addSite")
    @Transactional
    public ResponseEntity<Map<String, Object>> addSite(@RequestBody searchengine.config.Site newSite) {
        Map<String, Object> responseMap = new HashMap<>();

        if (siteRepository.existsByUrl(newSite.getUrl())) {
            return createErrorResponse("Сайт уже существует", responseMap);
        }

        searchengine.model.Site modelSite = createModelSite(newSite);
        siteRepository.save(modelSite);

        return createSuccessResponse(responseMap, "Сайт успешно добавлен: " + modelSite.getUrl());
    }

    @GetMapping("/startIndexing")
    @Transactional
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> responseMap = new HashMap<>();

        try {
            if (indexingService.isIndexingInProgress()) {
                return createErrorResponse("Индексация уже запущена", responseMap);
            }

            List<searchengine.config.Site> configSites = sitesList.getSites();
            logger.info("Найденные сайты для индексации: {}", configSites.size());

            configSites.forEach(configSite ->
                    siteRepository.findByUrl(configSite.getUrl()).ifPresent(existingSite -> {
                        pageRepository.deleteBySiteId(existingSite.getId());
                        siteRepository.delete(existingSite);
                        logger.info("Удалены старые данные для сайта: {}", existingSite.getUrl());
                    })
            );

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
        return createSuccessResponse(responseMap, "Индексация остановлена успешно");
    }

    private searchengine.model.Site createModelSite(searchengine.config.Site newSite) {
        searchengine.model.Site site = new searchengine.model.Site();
        site.setUrl(newSite.getUrl());
        site.setName(newSite.getName());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        return site;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, Map<String, Object> responseMap) {
        responseMap.put("result", false);
        responseMap.put("error", message);
        return ResponseEntity.badRequest().body(responseMap);
    }

    private ResponseEntity<Map<String, Object>> createSuccessResponse(Map<String, Object> responseMap, String message) {
        responseMap.put("result", true);
        responseMap.put("message", message);
        logger.info(message);
        return ResponseEntity.ok(responseMap);
    }
}