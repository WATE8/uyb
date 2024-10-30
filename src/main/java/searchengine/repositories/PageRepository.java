package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;

public interface PageRepository extends JpaRepository<Page, Long> {
    // Удаление страниц по идентификатору сайта
    void deleteBySiteId(Long siteId);

    // Проверка, существуют ли страницы для данного сайта
    boolean existsBySiteId(Long siteId);
}