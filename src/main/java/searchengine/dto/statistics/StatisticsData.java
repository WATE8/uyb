package searchengine.dto.statistics;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class StatisticsData {
    @NotNull
    private TotalStatistics total;

    @NotNull
    private List<DetailedStatisticsItem> detailed;
}