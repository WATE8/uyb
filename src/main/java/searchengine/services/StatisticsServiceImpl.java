package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String[] STATUSES = { "INDEXED", "FAILED", "INDEXING" };
    private static final String[] ERRORS = {
            "Ошибка индексации: главная страница сайта не доступна",
            "Ошибка индексации: сайт не доступен",
            null // Use null for no error
    };

    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = resetTotalStatistics();
        List<DetailedStatisticsItem> detailedStatistics = gatherDetailedStatistics(total);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(!detailedStatistics.isEmpty());
        response.setStatistics(constructStatisticsData(total, detailedStatistics));
        return response;
    }

    private TotalStatistics resetTotalStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.reset(); // Reset the statistics before collecting new data
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        return total;
    }

    private List<DetailedStatisticsItem> gatherDetailedStatistics(TotalStatistics total) {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        if (sitesList.isEmpty()) {
            return detailed; // Return empty if no sites exist
        }

        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = createDetailedStatisticsItem(site, i);
            detailed.add(item);
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
        }

        return detailed;
    }

    private DetailedStatisticsItem createDetailedStatisticsItem(Site site, int index) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        int pages = ThreadLocalRandom.current().nextInt(1_000);
        int lemmas = pages * ThreadLocalRandom.current().nextInt(1_000);
        item.setPages(pages);
        item.setLemmas(lemmas);
        item.setStatus(STATUSES[index % 3]);
        item.setError(ERRORS[index % 3]);
        item.setStatusTime(LocalDateTime.now().minusSeconds(ThreadLocalRandom.current().nextInt(10_000))); // Random status time
        return item;
    }

    private StatisticsData constructStatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        return data;
    }
}