package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // This annotation sets up the Spring context
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args); // Launches the application
    }
}