package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.repository.LemmaRepository;

@Service
public class LemmaService {

    @Autowired
    private LemmaRepository lemmaRepository;

    // Метод для нахождения леммы по ID
    public Lemma findById(Integer id) {
        return lemmaRepository.findById(id).orElse(null);  // Возвращаем null, если лемма не найдена
    }

    // Метод для обновления частоты леммы
    public void updateFrequency(Lemma lemma, Integer newFrequency, Integer maxFrequency) {
        lemma.setFrequency(newFrequency, maxFrequency);
        lemmaRepository.save(lemma);  // Сохраняем обновленную лемму в базе данных
    }
}
