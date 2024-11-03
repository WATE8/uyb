package searchengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "lemma")
@NoArgsConstructor // Lombok автоматически генерирует пустой конструктор
@AllArgsConstructor // Генерирует конструктор со всеми параметрами
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteBaza site; // Связь с объектом SiteBaza

    @Column(name = "lemma", nullable = false, length = 255)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    // Конструктор, который принимает объект SiteBaza
    public Lemma(SiteBaza site, String lemma, int frequency) {
        this.site = site; // Устанавливаем объект SiteBaza
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
