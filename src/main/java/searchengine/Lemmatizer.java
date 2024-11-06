package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.util.*;

public class Lemmatizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "и", "в", "на", "с", "у", "от", "по", "а", "но", "для", "без", "при", "до", "ни", "же", "или", "да", "то", "как", "к", "о", "об", "чтобы", "за", "что"
    );
    private final LuceneMorphology morphology;

    public Lemmatizer() throws Exception {
        // Инициализация LuceneMorphology для работы с русским языком
        morphology = new RussianLuceneMorphology();
    }

    // Метод для лемматизации текста с подсчётом частот лемм
    public HashMap<String, Integer> getLemmas(String text) {
        HashMap<String, Integer> lemmasCount = new HashMap<>();

        // Разделяем текст на слова, используя регулярное выражение для поиска всех буквенных последовательностей
        for (String word : text.split("\\P{IsAlphabetic}+")) {
            word = word.toLowerCase(); // Приводим слово к нижнему регистру

            // Пропускаем стоп-слова или пустые строки
            if (word.isEmpty() || STOP_WORDS.contains(word)) {
                continue;
            }

            // Получаем базовые формы слова (леммы) и выбираем первую из них
            List<String> baseForms = morphology.getNormalForms(word);
            if (!baseForms.isEmpty()) {
                String lemma = baseForms.get(0); // Берём первую базовую форму
                // Если лемма не является стоп-словом, добавляем её в карту частот
                if (!STOP_WORDS.contains(lemma)) {
                    lemmasCount.put(lemma, lemmasCount.getOrDefault(lemma, 0) + 1);
                }
            }
        }

        return lemmasCount; // Возвращаем карту с леммами и их частотами
    }

    // Метод для получения лемм для одного слова
    public List<String> getBaseForms(String word) {
        return morphology.getNormalForms(word.toLowerCase());
    }

    public static void main(String[] args) {
        try {
            Lemmatizer lemmatizer = new Lemmatizer();

            // Лемматизация одного слова (например, "леса")
            System.out.println("Леммы для слова 'леса':");
            List<String> wordBaseForms = lemmatizer.getBaseForms("леса");
            wordBaseForms.forEach(System.out::println);

            // Пример текста для лемматизации
            String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";

            // Получаем карту с леммами и их частотами
            HashMap<String, Integer> result = lemmatizer.getLemmas(text);

            // Выводим результаты
            System.out.println("\nЛеммы и их частоты в тексте:");
            result.forEach((lemma, count) -> System.out.println(lemma + " — " + count));
        } catch (Exception e) {
            System.err.println("Ошибка при лемматизации: " + e.getMessage());
        }
    }
}
