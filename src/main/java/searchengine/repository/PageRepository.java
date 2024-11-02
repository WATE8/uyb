package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;

public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySiteId(Integer siteId);
    Page findByPath(String path);
}
