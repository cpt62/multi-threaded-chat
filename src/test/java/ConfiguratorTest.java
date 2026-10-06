import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.Configurator;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfiguratorTest {
    @Test
    @DisplayName("Проверка получения настроек сервера")
    void shouldReturnValidProperties() {
        Properties properties = Configurator.geConfig();

        assertNotNull(properties, "Properties не должны быть null");
        assertTrue(properties.containsKey("host"), "Должен быть ключ 'host'");
        assertTrue(properties.containsKey("port"), "Должен быть ключ 'port'");
        assertTrue(properties.containsKey("backlog"), "Должен быть ключ 'backlog'");

    }
}
