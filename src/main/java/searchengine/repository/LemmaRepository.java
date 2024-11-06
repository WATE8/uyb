package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    // Вы можете добавить свои методы для поиска и работы с леммами, если нужно
}
