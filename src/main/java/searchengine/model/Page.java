package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "pages", indexes = @Index(name = "path_index", columnList = "path")) // Correct index definition
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id", nullable = false)
    private Integer siteId;

    @Column(nullable = false, columnDefinition = "TEXT") // Используйте TEXT для адреса страницы
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") // Используйте MEDIUMTEXT для контента страницы
    private String content;
}