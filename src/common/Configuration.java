package common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration class that is used to hold static properties of the application
 * and to access application settings that are held in the 'config.properties' file.
 *
 * @author 200008575
 */
public class Configuration {

    /**
     * Port that is used by the SFS to broadcast and receive messages across the
     * local network by instances of the SFS running on the network.
     */
    public static int MULTICAST_PORT = 22017;

    /**
     * Group address that is used by the SFS to broadcast and receive messages across the
     * local network by instances of the SFS running on the network.
     */
    public static String MULTICAST_GROUP = "231.0.0.0";

    /**
     * Path of the location of the configuration file. The default save
     * location is in the same directory as where the JAR is located.
     */
    private final String location = "config.properties";

    /**
     * Properties object that is used to represent the 'config.properties' file. The
     * file holds the settings on where the 'upload' and 'download' folder is.
     */
    private Properties properties;

    /**
     * Variable that holds the reference of this object that is used
     * when external callers need to access the configuration object.
     */
    private static final Configuration instance = new Configuration();

    /**
     * Configuration instantiation method.
     */
    private Configuration() {
        try (var defaultProperties = ResourceLoader.load("resources/config.properties")) {
            this.properties = new Properties();

            var file = new File(this.location);

            if (file.exists() && file.canRead() && file.canWrite()) {
                properties.load(new FileInputStream(file));
            } else {
                // load the properties from the jar provided configuration file
                properties.load(defaultProperties);
            }

            // If download folder doesn't exist or is set to nothing, then set it as the users home directory
            if (properties.getProperty("download") == null || properties.getProperty("download").equals("")) {
                properties.setProperty("download", System.getProperty("user.home"));
            }

            // If upload folder doesn't exist or is set to nothing, then set it as the users home directory
            if (properties.getProperty("upload") == null || properties.getProperty("upload").equals("")) {
                properties.setProperty("upload", System.getProperty("user.home"));
            }

            this.save();
        } catch (IOException e) {
            System.out.println("Couldn't load configuration file.");
        }
    }

    /**
     * Method to get an instance of the Configuration object
     *
     * @return A reference of this object.
     */
    public static Configuration getInstance() {
        return instance;
    }

    /**
     * Method to get a key that is represented in the properties object.
     *
     * @param key The name of the key to be accessed.
     * @return The value that is held by the key.
     * @throws IllegalArgumentException if the key doesn't exist in the properties
     *                                  object.
     */
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
     * @param key   - The name of the key to be set.
     * @param value - The new value that the key should store.
     * @throws IllegalArgumentException if the key doesn't exist in the properties
     *                                  object.
     */
    public void set(String key, String value) {
        if (!this.properties.containsKey(key)) {
            throw new IllegalArgumentException("Cannot access configuration property that doesn't exist.");
        }

        this.properties.setProperty(key, value);

        // save the properties to config file.
        // ensure that the passed value which is a path exists...
        var path = Paths.get(value);

        if (Files.notExists(path)) {
            throw new IllegalArgumentException(String.format("Folder '%s' doesn't exist.", value));
        }

        this.save();
    }

    /**
     * Private method to save the file to the provided location
     */
    private void save() {
        try {
            var file = new File(location);

            // If the file doesn't exist, then do nothing.
            if (!file.exists()) {
                boolean created = file.createNewFile();

                // If we fail to create the configuration save file, abort trying
                // to write to it.
                if (!created) {
                    System.out.println("Couldn't create configuration file.");
                    return;
                }
            }

            var writer = new PrintWriter(file);

            properties.store(writer, null);
        } catch (IOException e) {
            System.out.println("Couldn't save configuration file.");
        }
    }
}
