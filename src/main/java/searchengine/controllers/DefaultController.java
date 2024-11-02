package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;
import searchengine.repositories.SiteRepository;
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
    private final SitesList sitesList;

    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

    @PostMapping("/addSite")
    @Transactional
    public ResponseEntity<Map<String, Object>> addSite(@RequestBody searchengine.config.Site newSite) {
        Map<String, Object> response = new HashMap<>();

        // Проверка, существует ли сайт с таким URL
        if (siteRepository.findByUrl(newSite.getUrl()).isPresent()) {
            return createErrorResponse("Сайт уже существует", response);
        }

        // Сохранение нового сайта в базе данных
        searchengine.model.Site modelSite = createModelSite(newSite);
        siteRepository.save(modelSite);

        response.put("result", true);
        response.put("message", "Сайт успешно добавлен: " + modelSite.getUrl());
        logger.info("Добавлен новый сайт: {}", modelSite.getUrl());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/startIndexing")
    @Transactional
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (indexingService.isIndexingInProgress()) {
            return createErrorResponse("Индексация уже запущена", response);
        }

        // Получаем список сайтов из конфигурации
        List<searchengine.config.Site> configSites = sitesList.getSites();
        logger.info("Найденные сайты для индексации: {}", configSites);

        if (configSites.isEmpty()) {
            return createErrorResponse("Нет сайтов для индексации", response);
        }

        try {
            configSites.forEach(configSite -> {
                searchengine.model.Site site = createModelSite(configSite);
                siteRepository.save(site);
                logger.info("Старт индексации сайта: {}", site.getUrl());
            });

            indexingService.startFullIndexing();
            response.put("result", true);
            logger.info("Индексация успешно запущена для {} сайтов", configSites.size());
        } catch (Exception e) {
            logger.error("Ошибка во время процесса индексации: {}", e.getMessage());
            return createErrorResponse("Ошибка при запуске индексации: " + e.getMessage(), response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (!indexingService.isIndexingInProgress()) {
            return createErrorResponse("Индексация не запущена", response);
        }

        try {
            indexingService.stopIndexing();
            logger.info("Индексация остановлена");
            response.put("result", true);
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации: {}", e.getMessage());
            return createErrorResponse("Ошибка при остановке индексации: " + e.getMessage(), response);
        }

        return ResponseEntity.ok(response);
    }

    private searchengine.model.Site createModelSite(searchengine.config.Site configSite) {
        searchengine.model.Site site = new searchengine.model.Site();
        site.setUrl(configSite.getUrl());
        site.setName(configSite.getName());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        return site;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage, Map<String, Object> response) {
        response.put("result", false);
        response.put("error", errorMessage);
        return ResponseEntity.badRequest().body(response);
    }
}
