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
        morphology = new RussianLuceneMorphology();
    }

    // Метод для лемматизации одиночного слова
    public List<String> getBaseForms(String word) {
        return morphology.getNormalForms(word.toLowerCase());
    }

    // Метод для получения лемм с подсчётом частот в тексте
    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> lemmasCount = new HashMap<>();

        for (String word : text.split("\\P{IsAlphabetic}+")) {
            word = word.toLowerCase();

            if (word.isEmpty() || STOP_WORDS.contains(word)) {
                continue;
            }

            morphology.getNormalForms(word).stream()
                    .filter(lemma -> !STOP_WORDS.contains(lemma))
                    .findFirst()
                    .ifPresent(lemma -> lemmasCount.put(lemma, lemmasCount.getOrDefault(lemma, 0) + 1));
        }

        return lemmasCount;
    }

    public static void main(String[] args) {
        try {
            Lemmatizer lemmatizer = new Lemmatizer();

            // Лемматизация одного слова
            System.out.println("Леммы для слова 'леса':");
            List<String> wordBaseForms = lemmatizer.getBaseForms("леса");
            wordBaseForms.forEach(System.out::println);

            // Лемматизация текста с подсчётом частот
            String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
            System.out.println("\nЛеммы и их частоты в тексте:");
            Map<String, Integer> result = lemmatizer.getLemmas(text);

            result.forEach((lemma, count) -> System.out.println(lemma + " — " + count));
        } catch (Exception e) {
            System.err.println("Ошибка при лемматизации: " + e.getMessage());
        }
    }
}
