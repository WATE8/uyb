package searchengine.controllers;

import searchengine.services.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteController {

    @Autowired
    private SiteService siteService;

    @DeleteMapping("/deleteSiteData")
    public String deleteSiteData(@RequestParam Integer siteId) {
        siteService.deleteSiteData(siteId);
        return "Данные для сайта с ID " + siteId + " успешно удалены.";
    }
}