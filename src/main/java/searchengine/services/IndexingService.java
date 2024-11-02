package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);

    // Метод для проверки, идет ли индексация
    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

    // Метод для запуска индексации
    public void startIndexing() {
        if (indexingInProgress.compareAndSet(false, true)) {
            try {
                // Логика индексации (например, индексация сайтов)
                performIndexing();
            } finally {
                // Завершаем индексацию
                indexingInProgress.set(false);
            }
        } else {
            // Индексация уже запущена, выбрасываем исключение или обрабатываем по-другому
            throw new IllegalStateException("Индексация уже запущена");
        }
    }

    // Метод, который содержит логику индексации
    private void performIndexing() {
        // Здесь должна быть ваша логика индексации
        // Например, сбор данных с сайтов, обработка и индексация в Lucene
        System.out.println("Индексация начата...");
        // Имитация индексации
        try {
            Thread.sleep(5000); // Замените на реальную логику индексации
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Индексация завершена.");
    }
}