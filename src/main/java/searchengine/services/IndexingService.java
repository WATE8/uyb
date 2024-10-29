package searchengine.services;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    @Getter
    private boolean indexingInProgress = false;

    public void startFullIndexing() {
        indexingInProgress = true;

        new Thread(() -> {
            try {
                performIndexing();
            } finally {
                indexingInProgress = false;
            }
        }).start();
    }

    private void performIndexing() {
        // Логика для индексации всех сайтов
    }
}