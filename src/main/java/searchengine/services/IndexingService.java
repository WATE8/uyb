package searchengine.services;

import lombok.Getter;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;

@Service
public class IndexingService {

    @Getter
    private boolean indexingInProgress = false;

    private final SiteRepository siteRepository; // Assuming you have this repository
    private final PageRepository pageRepository; // Assuming you have this repository

    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public void startFullIndexing() {
        indexingInProgress = true;

        new Thread(() -> {
            try {
                performIndexing();
            } finally {
                indexingInProgress = false;
            }
        }).start();
    }

    private void performIndexing() {
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            pageRepository.deleteBySiteId(site.getId());

            // Example logic to create and save a page
            Page page = new Page();
            page.setSite(site); // Set the entire Site object
            page.setPath("/example-page"); // Example path
            page.setCode(200); // Example HTTP code
            page.setContent("Содержимое страницы"); // Example content

            pageRepository.save(page); // Save the new page
        }
    }
}