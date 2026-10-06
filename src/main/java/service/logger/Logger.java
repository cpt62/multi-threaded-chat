package service.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger {
    ConcurrentLinkedQueue<String> queue;
    Path path;
    Path file;
    AtomicInteger counter;
    long lastWriteTime;

    public Logger() {
        this.path = Path.of("logs");
        this.file = Path.of("logs", "chat.txt");
        createResources();
        queue = new ConcurrentLinkedQueue<>();
        this.counter = new AtomicInteger(0);
        lastWriteTime = System.currentTimeMillis();
        startLogger();
    }

    public void captureEvent(Events events, String msg) {
        String currentEvent = getTime() + ";" + events.name() + ";" + msg;
        if (queue.offer(currentEvent)) {
            int check = counter.getAndIncrement();
        } else {
            System.err.println("Ошибка буферизации события: " + currentEvent);
        }
    }

    public void startLogger() {
        Thread thread = new Thread(() -> {
            while (true) {
                if (counter.get() > 0 && (System.currentTimeMillis() - lastWriteTime > 5000 || counter.get() == 100)) {
                    int check = counter.getAndSet(0);
                    List<String> buffer = getLogsFromQueue(check);
                    if (!buffer.isEmpty()) {
                        writeToFile(buffer);
                        lastWriteTime = System.currentTimeMillis();
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Сохраняем статус прерывания
                    break; // Выходим из цикла while(true)
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void writeToFile(List<String> logs) {
        try (FileWriter writer = new FileWriter(file.toFile(), true)) {
            logs.forEach(s -> {
                try {
                    writer.write(s + "\n");
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка записи в лог");
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Файл отсутствует");
        }
    }


    private List<String> getLogsFromQueue(int current) {

        List<String> bufferScope = new ArrayList<>();
        for (int i = 0; i < current; i++) {
            String item = queue.poll();
            if (item != null) {
                bufferScope.add(item);
            }
        }
        return bufferScope;
    }

    private String getTime() {
        var dateRaw = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
        return dateRaw.format(formatter);
    }

    private void createResources() {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию. Проверьте корректность указанного пути");
        }
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось создать файл. Проверьте корректность указанного пути");
            }
        }
    }

    public void stop() {
        int remaining = counter.get();
        if (remaining > 0) {
            List<String> logs = getLogsFromQueue(remaining);
            if (!logs.isEmpty()) {
                writeToFile(logs);
            }
        }
    }
}