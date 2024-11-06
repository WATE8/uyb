package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import searchengine.model.Lemma;
import searchengine.services.LemmaService;

@RestController
@RequestMapping("/lemma")
public class LemmaController {

    @Autowired
    private LemmaService lemmaService;

    // Эндпоинт для обновления частоты леммы
    @PutMapping("/{lemmaId}")
    public String updateFrequency(
            @PathVariable Integer lemmaId,
            @RequestParam Integer newFrequency,
            @RequestParam Integer maxFrequency) {

        // Найдем лемму по её ID с использованием метода из сервиса
        Lemma lemma = lemmaService.findById(lemmaId);

        if (lemma == null) {
            return "Lemma not found";
        }

        // Обновим частоту леммы
        lemmaService.updateFrequency(lemma, newFrequency, maxFrequency);
        return "Frequency updated successfully for lemma with id: " + lemmaId;
    }
}
