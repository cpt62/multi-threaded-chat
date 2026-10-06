package server;

import service.Configurator;
import service.logger.Events;
import service.logger.Logger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static volatile Server server;
    private volatile boolean isRunning;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientHandler> concurrentHashMap;
    private ExecutorService executorService;
    private InetAddress host;
    private int port;
    private int backlog;
    private Logger logger;

    // Приватный конструктор, обеспечиваем работу с единственным экземпляром
    private Server() {
        getPropertiesFromConfigFile();
        try {
            serverSocket = new ServerSocket(port, backlog, host);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка инициализации ServerSocket. Проверьте конфигурационный файл.");
        }
        this.concurrentHashMap = new ConcurrentHashMap<>();
        System.out.println("Server started. Listen port: 8080 ...");
        executorService = Executors.newFixedThreadPool(5);
        this.logger = new Logger();
        isRunning = true;
    }

    public static Server getInstance() {
        if (server == null) {
            synchronized (Server.class) {
                if (server == null) {
                    server = new Server();
                    return server;
                }
            }
        }
        return server;
    }

    // служебный метод инициализации полей Server
    private void getPropertiesFromConfigFile() {
        Properties properties = Configurator.geConfig();
        try {
            this.host = InetAddress.getByName(properties.getProperty("host"));
            this.port = Integer.parseInt(properties.getProperty("port"));
            this.backlog = Integer.parseInt(properties.getProperty("backlog"));
        } catch (UnknownHostException e) {
            throw new RuntimeException("Не удалось устаносить хост подключения");
        }
    }

    private void startServer() {
        while (isRunning) {
            ClientHandler clientHandler = null;
            Socket socket = null;
            logger.captureEvent(Events.SERVER_STARTED, "Сервер запущен");
            try {
                socket = serverSocket.accept();
                if (socket != null && isRunning) {
                    clientHandler = new ClientHandler(socket, concurrentHashMap, logger);
                    executorService.submit(clientHandler::clientHandlerFlow);
                }
            } catch (IOException e) {
                if (!isRunning) {
                    logger.captureEvent(Events.SERVER_STOPPED, "Сервер остановлен");
                } else {
                    throw new RuntimeException("Сбой работы сервера!");
                }
            }
        }
    }

    private void stopServer() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Сервер уже остановлен");
        }
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Пул не успел завершить свою работу. Принудительная остановка!");
                executorService.shutdownNow();
                logger.captureEvent(Events.SERVER_STOPPED, "Сервер остановлен");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Server chatServer = Server.getInstance();
        Thread thread = new Thread(chatServer::startServer);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}