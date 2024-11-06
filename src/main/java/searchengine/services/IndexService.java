package searchengine.services;

import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.repository.IndexRepository;

@Service
public class IndexService {

    @Autowired
    private IndexRepository indexRepository;

    // Метод для обновления rank с использованием метода setRank
    public void updateRankForIndex(Integer indexId, Float newRank) {
        Index index = indexRepository.findById(indexId)
                .orElseThrow(() -> new NoSuchElementException("Index not found"));

        // Используем метод setRank с проверкой
        index.setRank(newRank);

        // Сохраняем обновленный объект
        indexRepository.save(index);
    }

    // Метод для создания нового индекса
    public Index createIndex(Integer pageId, Integer lemmaId, Float rank) {
        return new Index()
                .setPageId(pageId)
                .setLemmaId(lemmaId)
                .setRank(rank); // Здесь будет использоваться ваш метод setRank с проверкой
    }
}
