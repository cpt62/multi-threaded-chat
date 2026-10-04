package server;

import service.Configurator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class Server {
    private static volatile Server server;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientHandler> concurrentHashMap;
    private ExecutorService executorService;
    private InetAddress host;
    private int port;
    private int backlog;

    private Server() {
        getPropertiesFromConfigFile();
        try {
            serverSocket = new ServerSocket(port, backlog, host);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка инициализации ServerSocket. Проверьте конфигурационный файл.");
        }
        System.out.println("Server started. Listen port: 8080 ...");
    }

    public static Server getInstance() {
        if (server == null) {
            synchronized (Server.class) {
                if (server == null) {
                    server=new Server();
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

    public static void main(String[] args) {
        Server server1 = Server.getInstance();
    }
}
