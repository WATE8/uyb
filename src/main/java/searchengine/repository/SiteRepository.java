package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.SiteBaza;

public interface SiteRepository extends JpaRepository<SiteBaza, Integer> {
    // Вы можете добавить дополнительные методы для кастомных запросов здесь, если необходимо
}