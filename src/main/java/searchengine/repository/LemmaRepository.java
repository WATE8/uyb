package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    // Метод для поиска леммы по точному значению
    Lemma findByLemma(String lemma);

    // Метод для поиска лемм по частичному совпадению
    List<Lemma> findByLemmaContaining(String lemmaPart);

    // Метод для поиска лемм по частоте
    List<Lemma> findByFrequencyGreaterThanEqual(int frequency);

    // Метод для поиска лемм по ID сайта
    List<Lemma> findBySiteId(int siteId);
}
