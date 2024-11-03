package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import searchengine.model.Lemma;
import searchengine.services.LemmaService;

import java.util.List;

@RestController
@RequestMapping("/lemmas")
public class LemmaController {

    private final LemmaService lemmaService;

    @Autowired
    public LemmaController(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    @GetMapping("/site/{siteId}")
    public List<Lemma> getLemmasBySite(@PathVariable Integer siteId) { // Обновлено на Integer
        return lemmaService.getLemmasBySiteId(siteId);
    }

    @GetMapping("/search/{lemma}")
    public Lemma getLemma(@PathVariable String lemma) {
        return lemmaService.getLemmaByString(lemma);
    }
}
