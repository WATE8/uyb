package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.model.Status;

@Controller
public class DefaultController {

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @RequestMapping("/")
    public String index() {
        // Пример использования значений enum Status
        Status currentStatus = getCurrentStatus(); // Метод, который возвращает текущий статус

        // Используем метод getStatusMessage() для вывода сообщения о статусе
        String statusMessage = currentStatus.getStatusMessage();
        System.out.println(statusMessage); // Или можно использовать логирование

        // Логика в зависимости от статуса
        switch (currentStatus) {
            case INDEXED:
                // Логика для проиндексированного статуса
                break;
            case FAILED:
                // Логика для статуса с ошибкой
                break;
            case INDEXING:
                // Логика для статуса индексации
                break;
        }

        return "index";
    }

    // Метод для получения текущего статуса (можно адаптировать по необходимости)
    private Status getCurrentStatus() {
        // Здесь можно добавить логику для определения текущего статуса
        return Status.INDEXING; // Это временное значение
    }
}