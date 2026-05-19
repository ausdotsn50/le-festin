package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

    // Loads the file exactly once when the class is first referenced.
    // Tries the user-data directory first (written at runtime by the Setup Wizard),
    // then falls back to the classpath (bundled in the JAR for dev builds).
    static {
        try {
            java.nio.file.Path fsPath = AppDirs.configFilePath();
            InputStream input = Files.exists(fsPath)
                ? Files.newInputStream(fsPath)
                : ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

            if (input == null) {
                throw new ExceptionInInitializerError(
                    "[ConfigLoader] '" + CONFIG_FILE + "' not found.\n" +
                    "  → Run the app once so the Setup Wizard can create it at:\n" +
                    "  → " + AppDirs.configFilePath()
                );
            }

            try (input) {
                props.load(input);
            }
            System.out.println("[ConfigLoader] config.properties loaded successfully.");

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "[ConfigLoader] Failed to read " + CONFIG_FILE + ": " + e.getMessage()
            );
        }
    }

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