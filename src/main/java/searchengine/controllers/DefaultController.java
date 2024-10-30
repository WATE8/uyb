package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;
import searchengine.services.IndexingService;

@Controller
@RequiredArgsConstructor
public class DefaultController {
    private final IndexingService indexingService;

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        Map<String, Object> response = createResponse();

        if (indexingService.isIndexingInProgress()) {
            return buildErrorResponse(response, "Индексация уже запущена");
        }

        indexingService.startFullIndexing();
        response.put("result", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String, Object> response = createResponse();

        if (!indexingService.isIndexingInProgress()) {
            return buildErrorResponse(response, "Индексация не запущена");
        }

        indexingService.stopIndexing();
        response.put("result", true);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(Map<String, Object> response, String errorMessage) {
        response.put("result", false);
        response.put("error", errorMessage);
        return ResponseEntity.badRequest().body(response);
    }

    private Map<String, Object> createResponse() {
        return new HashMap<>();
    }
}
