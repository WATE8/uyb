package searchengine.controllers;

import java.util.NoSuchElementException;  // Добавьте этот импорт в начало файла
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import searchengine.model.Index;
import searchengine.services.IndexService;

@RestController
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private IndexService indexService;

    // Эндпоинт для обновления rank
    @PutMapping("/{indexId}")
    public String updateRank(@PathVariable Integer indexId, @RequestParam Float newRank) {
        try {
            indexService.updateRankForIndex(indexId, newRank);
            return "Rank updated successfully";
        } catch (NoSuchElementException e) {
            return "Index not found";
        }
    }

    // Эндпоинт для создания нового индекса
    @PostMapping
    public Index createIndex(@RequestParam Integer pageId, @RequestParam Integer lemmaId, @RequestParam Float rank) {
        return indexService.createIndex(pageId, lemmaId, rank);
    }
}
