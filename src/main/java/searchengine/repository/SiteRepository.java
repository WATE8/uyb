package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.SiteBaza;

public interface SiteRepository extends JpaRepository<SiteBaza, Integer> {
    SiteBaza findByUrl(String url);
}
