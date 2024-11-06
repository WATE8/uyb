package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id", nullable = false)
    private Integer siteId;

    @Column(name = "lemma", nullable = false, length = 255)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    // Метод для установки frequency с проверкой maxFrequency
    public void setFrequency(Integer frequency, Integer maxFrequency) {
        if (frequency > maxFrequency) {
            throw new IllegalArgumentException("Frequency cannot exceed maximum allowed value");
        }
        this.frequency = frequency;
    }
}
