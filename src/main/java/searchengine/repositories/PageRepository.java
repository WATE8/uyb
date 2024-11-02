package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySiteId(Integer siteId); // Изменено на Integer

    boolean existsBySiteId(Integer siteId); // Изменено на Integer

    boolean existsBySiteAndPath(Site site, String path);
}