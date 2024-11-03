package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteBaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // Статус индексации (INDEXING, INDEXED, FAILED)

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime; // Дата и время статуса

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError; // Текст ошибки индексации или NULL

    @Column(nullable = false, length = 255) // Длина URL ограничена 255 символами
    private String url; // Адрес главной страницы сайта

    @Column(nullable = false, length = 255) // Длина имени сайта ограничена 255 символами
    private String name; // Имя сайта
}
