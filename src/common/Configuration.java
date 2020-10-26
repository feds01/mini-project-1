package common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration class that is used to hold static properties of the application
 * and to access application settings that are held in the 'config.properties' file.
 *
 * @author 200008575
 * */
public class Configuration {

    /**
     * Port that is used by the SFS to broadcast and receive messages across the
     * local network by instances of the SFS running on the network.
     * */
    public static int MULTICAST_PORT = 22017;

    /**
     * Group address that is used by the SFS to broadcast and receive messages across the
     * local network by instances of the SFS running on the network.
     * */
    public static String MULTICAST_GROUP = "231.0.0.0";

    /**
     * Properties object that is used to represent the 'config.properties' file. The
     * file holds the settings on where the 'upload' and 'download' folder is.
     * */
    private Properties properties;

    /**
     * Variable that holds the reference of this object that is used
     * when external callers need to access the configuration object.
     * */
    private static final Configuration instance = new Configuration();

    /**
     * Configuration instantiation method.
     * */
    private Configuration() {
        try (var input = this.getClass().getClassLoader().getResourceAsStream("config.properties")) {
            this.properties = new Properties();

            if (input == null) {
                throw new RuntimeException("Failed to load properties.");
            }

            // load the properties from the configuration file
            properties.load(input);

            var saveRequired = false;

            // set the default download and upload folders as the users home directory
            if (properties.getProperty("download").equals("")) {
                properties.setProperty("download", System.getProperty("user.home"));
                saveRequired = true;
            }

            if (properties.getProperty("upload").equals("")) {
                properties.setProperty("upload", System.getProperty("user.home"));
                saveRequired = true;
            }

            // If we need to overwrite the default settings of the application, then do so
            if (saveRequired) {
                var output =Files.newBufferedWriter(Paths.get(this.getClass().getClassLoader().getResource("config.properties").toURI()));

                properties.store(output, null);
            }

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to get an instance of the Configuration object
     *
     * @return A reference of this object.
     * */
    public static Configuration getInstance() {
        return instance;
    }

    /**
     * Method to get a key that is represented in the properties object.
     *
     * @param key The name of the key to be accessed.
     * @return The value that is held by the key.
     *
     * @throws IllegalArgumentException if the key doesn't exist in the properties
     *                                  object.
     * */
    public String get(String key) {
        if (!this.properties.containsKey(key)) {
            throw new IllegalArgumentException("Cannot access configuration property that doesn't exist.");
        }

        return this.properties.getProperty(key);
    }

    /**
     * Method to set a key with a value that is represented in the properties object.
     * When this method is called, the properties object is saved to 'config.properties'
     * to ensure that the settings persist.
     *
     * @param key - The name of the key to be set.
     * @param value - The new value that the key should store.
     *
     * @throws IllegalArgumentException if the key doesn't exist in the properties
     *                                  object.
     * */
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
