package searchengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "page", indexes = @Index(name = "idx_path", columnList = "path"))
@Data
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Изменено на Integer

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @NotNull
    @Column(nullable = false)
    private int code;

    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    // Constructor for creating a new Page instance
    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}