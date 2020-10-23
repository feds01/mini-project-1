package common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Configuration {
    private Properties properties;
    private static final Configuration instance = new Configuration();


    private Configuration() {
        try (var input = this.getClass().getClassLoader().getResourceAsStream("config.properties")) {
            this.properties = new Properties();

            if (input == null) {
                throw new RuntimeException("Failed to load properties.");
            }

            // load the properties from the configuration file
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Configuration getInstance() {
        return instance;
    }

    public String get(String key) {
        if (!this.properties.containsKey(key)) {
            throw new IllegalArgumentException("Cannot access configuration property that doesn't exist.");
        }

        return this.properties.getProperty(key);
    }

    public void set(String key, String value) {
        if (!this.properties.containsKey(key)) {
            throw new IllegalArgumentException("Cannot access configuration property that doesn't exist.");
        }

        this.properties.setProperty(key, value);

        // save the properties to config file.
        try (
            var output = Files.newBufferedWriter(Paths.get(this.getClass().getClassLoader().getResource("config.properties").toURI()))
        ) {

            // ensure that the passed value which is a path exists...
            var path = Paths.get(value);

            if (Files.notExists(path)) {
                throw new IllegalArgumentException(String.format("Folder '%s' doesn't exist.", value));
            }

            this.properties.store(output, null);

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

}
