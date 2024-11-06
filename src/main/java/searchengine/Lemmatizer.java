package searchengine;

import java.util.*;
import org.apache.lucene.morphology.russian.RussianMorphology;

public class Lemmatizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "и", "в", "на", "с", "у", "от", "по", "а", "но", "для", "без", "при", "до", "ни", "же", "или", "да", "то", "как", "к", "о", "об", "чтобы", "за", "что"
    );
    private final RussianMorphology morphology;

    public Lemmatizer() throws Exception {
        morphology = new RussianMorphology();
    }

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

    public static void main(String[] args) throws Exception {
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        Lemmatizer lemmatizer = new Lemmatizer();
        Map<String, Integer> result = lemmatizer.getLemmas(text);

        result.forEach((lemma, count) -> System.out.println(lemma + " — " + count));
    }
}
