package searchengine.dto.statistics;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedStatisticsItem {
    @NotNull
    private String url;

    @NotNull
    private String name;

    private String status;
    private LocalDateTime statusTime; // Changed to LocalDateTime
    private String error;
    private int pages;
    private int lemmas;
}