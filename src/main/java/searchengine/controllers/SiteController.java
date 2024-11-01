package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import searchengine.services.SiteService;

@RestController
@RequestMapping("/sites")
public class SiteController {

    private final SiteService siteService;

    @Autowired
    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @PostMapping("/process")
    public String processSite(@RequestParam String url) {
        siteService.processSite(url);
        return "Processing site with URL: " + url;
    }
}
