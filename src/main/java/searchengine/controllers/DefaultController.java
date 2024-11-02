package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // Импорт для параметров запроса
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
    public ResponseEntity<?> startIndexing(@RequestParam(value = "depth", defaultValue = "1") int depth) { // Параметр глубины
        if (indexingService.isIndexingInProgress()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }

        try {
            indexingService.startIndexing(depth); // Передаем глубину в метод запуска индексации
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", false, "error", "Ошибка при запуске индексации: " + e.getMessage()));
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
