package com.lefestin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigLoader — loads config.properties once and exposes typed getters.
 *
 * Usage:
 *   String url = ConfigLoader.get("db.url");
 *   int timeout = ConfigLoader.getInt("db.timeout", 30);
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();

    // Loads the file exactly once when the class is first referenced
    static {
        try (InputStream input = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new ExceptionInInitializerError(
                    "[ConfigLoader] '" + CONFIG_FILE + "' not found in src/main/resources/.\n" +
                    "  → Copy config.properties.example to src/main/resources/config.properties\n" +
                    "  → Fill in your db.url, db.user, and db.password"
                );
            }

            props.load(input);
            System.out.println("[ConfigLoader] config.properties loaded successfully.");

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "[ConfigLoader] Failed to read " + CONFIG_FILE + ": " + e.getMessage()
            );
        }
    }
    
    private ConfigLoader() {}

    /**
     * Returns the value for the given key.
     * Throws if the key is missing or blank — fail fast rather than NPE later.
     */
    public static String get(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new RuntimeException(
                "[ConfigLoader] Missing required property: '" + key + "' in " + CONFIG_FILE
            );
        }
        return value.trim();
    }

    /**
     * Returns the value for the given key, or a default if the key is absent.
     * Use this for optional settings like timeouts or pool sizes.
     */
    public static String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Returns an integer property. Useful for port numbers, timeouts, pool sizes.
     */
    public static int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                "[ConfigLoader] Property '" + key + "' must be an integer, got: '" + value + "'"
            );
        }
    }
}