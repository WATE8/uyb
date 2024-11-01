package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
    // Find a Site by its URL
    Site findByUrl(String url);

    // Additional custom methods can be added as needed
}