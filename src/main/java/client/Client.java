package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private String name;
    private InetAddress host;
    private int port;
    private BufferedReader console;
    private BufferedReader br;
    private PrintWriter pw;
    private volatile boolean isRunning;

    public Client(String name) {
        getPropertiesFromConfigFile();
        try {
            this.socket = new Socket(host, port);
            this.name = name;
            console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            isRunning = true;
        } catch (IOException e) {
            throw new RuntimeException("Неудачная попытка соединения с сервером");
        }
    }

    public String getName() {
        return name;
    }

    public void start() {
        pw.println(getName());

        Thread thread = new Thread(() -> {
            String msgFromSrv;
            while (isRunning) {
                try {
                    if ((msgFromSrv = br.readLine()) != null) {
                        System.out.println(msgFromSrv);
                    }
                } catch (IOException e) {
                    System.err.println("Соединение с сервером потеряно");
                    stop();
                }
            }
        });
        thread.start();

        String msgToSrv;
        while (isRunning) {
            try {
                if ((msgToSrv = console.readLine()) != null) {
                    if (msgToSrv.equals("/exit")) {
                        stop();
                        break;
                    }
                    pw.println(msgToSrv);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException("Client был закрыт ранее");
            }
        }
    }

    private void getPropertiesFromConfigFile() {
        Properties properties = new Properties();

        InputStream is = Client.class.getClassLoader().getResourceAsStream("configuration.properties");
        try {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось устаносить хост подключения");
        }

        try {
            this.host = InetAddress.getByName(properties.getProperty("host"));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.port = Integer.parseInt(properties.getProperty("port"));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        Client client = new Client(name);
        Thread thread = new Thread(client::start);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
