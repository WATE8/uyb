package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Long> { // Keep Long for ID
    // Удаление страниц по идентификатору сайта
    void deleteBySiteId(Long siteId); // Use Long

    // Проверка, существуют ли страницы для данного сайта
    boolean existsBySiteId(Long siteId); // Use Long

    // Проверка, существует ли страница для данного сайта и пути
    boolean existsBySiteAndPath(Site site, String path);
}