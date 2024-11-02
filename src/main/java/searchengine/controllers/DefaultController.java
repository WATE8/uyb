package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.services.IndexingService;

import java.util.Map;

@Controller
public class DefaultController {

    private final IndexingService indexingService;

    @Autowired
    public DefaultController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<?> startIndexing(@RequestParam(value = "depth", defaultValue = "1") int depth) {
        if (indexingService.isIndexingInProgress()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }

        try {
            indexingService.startIndexing(depth);
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", false, "error", "Ошибка при запуске индексации: " + e.getMessage()));
        }
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        if (!indexingService.isIndexingInProgress()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация не запущена"));
        }

        try {
            indexingService.stopIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", false, "error", "Ошибка при остановке индексации: " + e.getMessage()));
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }
}