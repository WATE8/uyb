package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import searchengine.model.Lemma; // Убедитесь, что у вас есть импорт модели Lemma
import searchengine.services.IndexingService;

import java.util.List;
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

    // Конечная точка для получения всех лемм
    @GetMapping("/api/lemmas")
    public ResponseEntity<List<Lemma>> getAllLemmas() {
        List<Lemma> lemmas = indexingService.findAllLemmas();
        return ResponseEntity.ok(lemmas);
    }

    // Конечная точка для получения леммы по ID
    @GetMapping("/api/lemmas/{id}")
    public ResponseEntity<Lemma> getLemmaById(@PathVariable int id) {
        Lemma lemma = indexingService.findLemmaById(id);
        return lemma != null ? ResponseEntity.ok(lemma) : ResponseEntity.notFound().build();
    }

    // Конечная точка для создания новой леммы
    @PostMapping("/api/lemmas")
    public ResponseEntity<Lemma> createLemma(@RequestBody Lemma lemma) {
        Lemma createdLemma = indexingService.createLemma(lemma);
        return ResponseEntity.ok(createdLemma);
    }

    // Конечная точка для удаления леммы по ID
    @DeleteMapping("/api/lemmas/{id}")
    public ResponseEntity<Void> deleteLemma(@PathVariable int id) {
        indexingService.deleteLemmaById(id);
        return ResponseEntity.noContent().build();
    }
}
