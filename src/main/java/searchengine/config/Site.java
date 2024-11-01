package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site {
    private String url;
    private String name;

    // Constructor for easy initialization
    public Site(String url, String name) {
        this.url = url;
        this.name = name;
    }
}