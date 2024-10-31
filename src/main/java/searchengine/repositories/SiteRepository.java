package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
    // Здесь можно добавить дополнительные методы, если это необходимо
}