package service;

import java.io.IOException;
import java.util.Properties;

public class Configurator {
    public static Properties geConfig() {
        Properties properties = new Properties();
        var inputStream = Configurator.class.getClassLoader().getResourceAsStream("configuration.properties");
        if (inputStream == null) throw new RuntimeException("Файл \"configuration.properties\" не найден");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка ввода вывода при получении properties. Проверьте файл конфигурации.");
        }
        return properties;
    }
}
