package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.repository.LemmaRepository;

import java.util.List;

@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    // Метод для получения лемм по ID сайта
    public List<Lemma> getLemmasBySiteId(Integer siteId) { // Обновлено на Integer
        return lemmaRepository.findBySiteId(siteId);
    }

    // Метод для получения леммы по слову
    public Lemma getLemmaByString(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }
}
