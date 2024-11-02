package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Integer> {
    // Удаляет все страницы по ID сайта
    void deleteBySiteId(Integer siteId);

    // Проверяет, существуют ли страницы для данного сайта
    boolean existsBySiteId(Integer siteId);

    // Проверяет, существует ли страница с указанным сайтом и путем
    boolean existsBySiteAndPath(Site site, String path);
}
