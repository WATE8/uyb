package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;

    // No-args constructor initializes all values to default
    public TotalStatistics() {
        this.sites = 0;
        this.pages = 0;
        this.lemmas = 0;
        this.indexing = false;
    }

    // Optional method to reset statistics
    public void reset() {
        this.sites = 0;
        this.pages = 0;
        this.lemmas = 0;
        this.indexing = false;
    }

    @Override
    public String toString() {
        return "TotalStatistics{" +
                "sites=" + sites +
                ", pages=" + pages +
                ", lemmas=" + lemmas +
                ", indexing=" + indexing +
                '}';
    }
}