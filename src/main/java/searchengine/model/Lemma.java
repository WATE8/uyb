package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lemma {
    @Id
    @Min(value = 1, message = "ID леммы должен быть положительным")
    private int id; // ID леммы

    @Min(value = 1, message = "ID сайта должен быть положительным")
    private int siteId; // ID веб-сайта

    @NotBlank(message = "Лемма не может быть пустой")
    @Max(value = 255, message = "Длина леммы не может превышать 255 символов")
    private String lemma; // Нормальная форма слова

    @Min(value = 0, message = "Частота не может быть отрицательной")
    private int frequency; // Частота упоминаний

    public void updateFrequency(int newFrequency) {
        if (newFrequency < 0) {
            throw new IllegalArgumentException("Частота не может быть отрицательной");
        }
        this.frequency = newFrequency;
    }
}
