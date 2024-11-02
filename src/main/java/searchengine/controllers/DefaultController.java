package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import searchengine.services.IndexingService; // Предполагается, что у вас есть такой сервис
import java.util.Map;

@Controller // Изменяем на Controller
public class DefaultController {

    private final IndexingService indexingService; // Сервис для индексации

    @Autowired
    public DefaultController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingService.isIndexingInProgress()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }

        indexingService.startIndexing(); // Метод запуска индексации
        return ResponseEntity.ok(Map.of("result", true));
    }

    @GetMapping("/") // Оставляем этот метод для корневого маршрута
    public String index() {
        return "index"; // Возврат HTML-страницы
    }
}