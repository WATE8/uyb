package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site {
    private String url;
    private String name;

    // Пустой конструктор для совместимости с Spring
    public Site() {
    }

    // Конструктор для удобства инициализации
    public Site(String url, String name) {
        this.url = url;
        this.name = name;
    }
}