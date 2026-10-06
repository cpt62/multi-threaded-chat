import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.logger.Events;
import service.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggerTest {
    private Logger logger;
    private Path logFile = Path.of("logs", "chat.txt");

    @BeforeEach
    void start() {
        try {
            Files.deleteIfExists(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            logger = new Logger();
        }
    }

    @AfterEach
    void stop() {
        logger.stop();
    }

    @Test
    @DisplayName("Проверка записи в chat.txt")
    void shouldWriteToLogFile() {
        logger.captureEvent(Events.SERVER_STARTED, "Тестовое сообщение: запуск сервера!");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(Files.exists(logFile), "Файл chat.txt должен быть создан");

        String value = null;
        try {
            value = Files.readString(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertTrue(value.contains("SERVER_STARTED"), "В логе должно быть событие: SERVER_STARTED");
        assertTrue(value.contains("Тестовое сообщение: запуск сервера!"), "В логе должно быть сообщение: Тестовое сообщение: запуск сервера!");
    }
}
