import common.Configuration;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationTests {
    public static Configuration config = Configuration.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {"upload", "download"})
    public void testGetWithValidKey(String key) {
        assertDoesNotThrow(() -> {
            config.get(key);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"u", "down", "", "   "})
    public void testGetWithInvalidKey(String key) {
        assertThrows(IllegalArgumentException.class, () -> {
            config.get(key);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"upload", "download"})
    public void testSetWithValidKey(String key) {
        assertDoesNotThrow(() -> {
            config.set(key, System.getProperty("user.home"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"u", "down", "", "   "})
    public void testSetWithInvalidKey(String key) {
        assertThrows(IllegalArgumentException.class, () -> {
            config.set(key, " ");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"u", "down", "   ", "?"})
    public void testSetWithInvalidValues(String value) {
        assertThrows(IllegalArgumentException.class, () -> {
            config.set("download", value);
        });
    }
}
