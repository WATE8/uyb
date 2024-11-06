package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true) // Устанавливает методы fluent-style
@Entity
@Table(name = "index")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "page_id", nullable = false)
    private Integer pageId;

    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;

    @Setter(AccessLevel.NONE) // Отключение автогенерации сеттера для rank
    @Column(name = "rank", nullable = false)
    private Float rank;

    // Метод с проверкой
    public Index setRank(Float rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("Rank cannot be negative");
        }
        this.rank = rank;
        return this; // Позволяет использовать цепочку вызовов
    }
}
