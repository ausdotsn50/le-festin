package config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AppDirs — resolves user-writable application data paths.
 * No static initializer; safe to call before any config file exists.
 */
public class AppDirs {

    private static final String APP_NAME = "LeFestin";

    /**
     * Returns the path to config.properties in the OS-appropriate user data dir.
     * Windows : %APPDATA%\LeFestin\config.properties
     * macOS   : ~/Library/Application Support/LeFestin/config.properties
     * Linux   : $XDG_CONFIG_HOME/LeFestin/config.properties  (default ~/.config)
     */
    public static Path configFilePath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path dir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            dir = (appData != null ? Paths.get(appData)
                                   : Paths.get(System.getProperty("user.home"), "AppData", "Roaming"))
                  .resolve(APP_NAME);
        } else if (os.contains("mac")) {
            dir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", APP_NAME);
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            dir = (xdg != null && !xdg.isBlank() ? Paths.get(xdg)
                                                  : Paths.get(System.getProperty("user.home"), ".config"))
                  .resolve(APP_NAME);
        }
        return dir.resolve("config.properties");
    }

    private AppDirs() {}
}