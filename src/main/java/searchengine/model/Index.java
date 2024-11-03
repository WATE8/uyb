package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "`index`") // Имя таблицы в базе данных с использованием обратных кавычек
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Идентификатор индекса

    @Column(name = "page_id", nullable = false)
    private Long pageId; // Идентификатор страницы

    @Column(name = "lemma_id", nullable = false)
    private Long lemmaId; // Идентификатор леммы

    @Column(name = "rank", nullable = false)
    private float rank; // Количество данной леммы для данной страницы
}
