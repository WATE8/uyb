package searchengine.services;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PageIndexer {

    private final Directory directory; // Directory для хранения индекса
    private final IndexWriterConfig config;

    public PageIndexer() throws IOException {
        this.directory = FSDirectory.open(java.nio.file.Paths.get("index"));
        this.config = new IndexWriterConfig();
    }

    // Индексация страницы по URL
    public void indexPage(String url) throws IOException {
        Document htmlDoc = fetchDocument(url); // Получаем HTML-документ по URL
        String content = htmlDoc.text(); // Извлекаем текстовый контент из HTML

        // Индексируем контент
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            // Удаляем старую запись, если она уже существует
            writer.deleteDocuments(new Term("url", url));

            // Создаем новый документ Lucene
            org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
            luceneDoc.add(new StringField("url", url, Field.Store.YES));
            luceneDoc.add(new TextField("content", content, Field.Store.YES));
            writer.addDocument(luceneDoc); // Добавляем новый документ
            writer.commit(); // Применяем изменения
        }
    }

    // Получаем HTML-документ с указанного URL
    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url).get(); // Используем Jsoup для получения HTML страницы
    }
}
