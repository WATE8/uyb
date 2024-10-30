package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Long> {
    // Удаление страниц по идентификатору сайта
    void deleteBySiteId(Long siteId);

    // Проверка, существуют ли страницы для данного сайта
    boolean existsBySiteId(Long siteId);

    // Проверка, существует ли страница для данного сайта и пути
    boolean existsBySiteAndPath(Site site, String path);
}