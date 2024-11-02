package searchengine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor // Генерирует конструктор по умолчанию
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @NotNull
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @NotNull
    @Column(unique = true, nullable = false) // Уникальный индекс на поле url
    private String url;

    @NotNull
    @Column(nullable = false)
    private String name;

    // Конструктор с параметрами
    public Site(String url, String name, Status status, LocalDateTime statusTime) {
        this.url = url;
        this.name = name;
        this.status = status;
        this.statusTime = statusTime;
    }

    // Метод для обновления статуса
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
        this.statusTime = LocalDateTime.now(); // Обновление времени статуса
    }
}
