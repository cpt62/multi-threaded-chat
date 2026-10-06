package server;

import service.logger.Events;
import service.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler {
    private String name;
    private Socket socket;
    private volatile boolean isClosed;
    private ConcurrentHashMap<String, ClientHandler> concurrentHashMap;
    private BufferedReader br;
    private PrintWriter pw;
    private Logger logger;

    public String getName() {
        return name;
    }

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> concurrentHashMap, Logger logger) {
        this.socket = socket;
        this.concurrentHashMap = concurrentHashMap;
        this.logger = logger;
        try {
            this.pw = new PrintWriter(socket.getOutputStream(), true);
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        isClosed = false;
    }

    private void stop() {
        try {
            socket.close();
            isClosed = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTime() {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    public void clientHandlerFlow() {
        try {
            this.name = this.br.readLine();
            if (name == null) {
                stop();
                return;
            }
            logger.captureEvent(Events.CLIENT_CONNECTED, "Клиент: " + name + " подключен");
            concurrentHashMap.putIfAbsent(name, this);
            pw.println(getTime() + " | Добро пожаловать в чат,  " + name);
            while (!isClosed) {
                String msg;
                if ((msg = br.readLine()) != null) {
                    if (msg.isEmpty()) continue;
                    if (msg.equals("/exit")) break;
                    String currTime = getTime();
                    logger.captureEvent(Events.CLIENT_MESSAGE, "Клиент: " + name + " отправил сообщение: " + msg);
                    concurrentHashMap
                            .values()
                            .forEach(ch -> {
                                if (!ch.name.equals(this.name))
                                    ch.pw.println(currTime + " | " + name + " | " + msg);
                            });
                }
            }
            stop();
            logger.captureEvent(Events.CLIENT_DISCONNECTED, "Клиент: " + name + " отключен");
            concurrentHashMap.remove(name);
        } catch (IOException e) {
            if (this.socket.isClosed()) {
                if (name != null) {
                    System.out.println("Клиент " + name + " покинул чат");
                } else {
                    System.out.println("Клиент не назвавший своё имя, покинул чат");
                }
            } else {
                if (name != null) {
                    System.err.println("Соединение клиента " + name + " разорвано");
                } else
                    System.err.println("Соединение разорвано. Имя клиента неизвестно");
            }
        } finally {
            if (name != null)
                concurrentHashMap.remove(name);
        }
    }

}
