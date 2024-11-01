package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing(
            @RequestParam String url,
            @RequestParam String name) {
        Map<String, Object> response = new HashMap<>();

        if (indexingService.isIndexingInProgress()) {
            return createErrorResponse("Индексация уже запущена", response);
        }

        try {
            Site newSite = createNewSite(url, name);
            saveSite(newSite);
            logger.info("Site added: {}", newSite.getUrl());

            indexingService.startFullIndexing();
            response.put("result", true);
        } catch (Exception e) {
            logger.error("Error during indexing process: {}", e.getMessage());
            return createErrorResponse("Ошибка при запуске индексации", response);
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
            logger.info("Indexing stopped");
            response.put("result", true);
        } catch (Exception e) {
            logger.error("Error stopping indexing: {}", e.getMessage());
            return createErrorResponse("Ошибка при остановке индексации", response);
        }

        return ResponseEntity.ok(response);
    }

    private void saveSite(Site site) {
        siteRepository.save(site);
    }

    private Site createNewSite(String url, String name) {
        Site site = new Site();
        site.setUrl(url);
        site.setName(name);
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