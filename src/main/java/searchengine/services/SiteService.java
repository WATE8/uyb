package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    @Autowired
    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public Site getSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    public void processSite(String url) {
        Site site = getSiteByUrl(url);
        if (site != null) {
            // Perform operations on the site
            System.out.println("Processing site: " + site.getName());
            // Additional processing logic can go here
        } else {
            // Handle the case where the site is not found
            System.out.println("Site not found for URL: " + url);
        }
    }
}