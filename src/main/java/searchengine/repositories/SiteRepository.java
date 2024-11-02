package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url); // Поиск сайта по URL

    // Метод для проверки существования сайта с данным URL
    boolean existsByUrl(String url);
}
