package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import searchengine.services.IndexingService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class DefaultController {

    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> response = new HashMap<>();

        if (indexingService.isIndexingInProgress()) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return ResponseEntity.badRequest().body(response);
        }

        indexingService.startFullIndexing();
        response.put("result", true);
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

        indexingService.stopIndexing();
        response.put("result", true);
        return ResponseEntity.ok(response);
    }
}