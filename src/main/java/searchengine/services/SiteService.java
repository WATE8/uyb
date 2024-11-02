package searchengine.services;

import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Transactional
    public void deleteSiteData(Integer siteId) {
        pageRepository.deleteBySiteId(siteId); // Удаление всех страниц по siteId
        siteRepository.deleteById(siteId); // Удаление записи о сайте
    }
}