package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        try {
            StatisticsResponse response = statisticsService.getStatistics();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception (you could use a logging framework like SLF4J)
            return ResponseEntity.internalServerError().build(); // 500 error
        }
    }
}