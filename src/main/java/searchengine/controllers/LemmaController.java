package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import searchengine.model.Lemma;
import searchengine.services.LemmaService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/lemmas")
public class LemmaController {

    private final LemmaService lemmaService;

    @Autowired
    public LemmaController(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<Lemma>> getLemmasBySite(@PathVariable Integer siteId) {
        List<Lemma> lemmas = lemmaService.getLemmasBySiteId(siteId);
        if (lemmas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(lemmas); // Возвращаем 404 если не найдено
        }
        return ResponseEntity.ok(lemmas); // Возвращаем 200 и список лемм
    }

    @GetMapping("/search/{lemma}")
    public ResponseEntity<Lemma> getLemma(@PathVariable String lemma) {
        Lemma foundLemma = lemmaService.getLemmaByString(lemma);
        if (foundLemma == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Возвращаем 404 если не найдено
        }
        return ResponseEntity.ok(foundLemma); // Возвращаем 200 и найденную лемму
    }
}
