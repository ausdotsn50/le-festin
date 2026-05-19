import java.io.File;

import javax.swing.SwingUtilities;

import config.AppDirs;
import config.DBConnection;
import ui.AppTheme;
import ui.MainFrame;
import ui.dialogs.LoginDialog;
import ui.dialogs.SetupWizardDialog;

public class Main {
    public static void main(String[] args) {
        AppTheme.install();

        SwingUtilities.invokeLater(() -> {
            // First-run wizard BEFORE MainFrame is constructed.
            // MainFrame eagerly builds all panels, which touch DBConnection → ConfigLoader.
            // If config.properties is missing at that point, ConfigLoader's static
            // initializer fails permanently (NoClassDefFoundError on every later access).
            File configFile = AppDirs.configFilePath().toFile();
            
            // FIX: Double-check the parent directory exists so the SetupWizard can safely write to it.
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!configFile.exists() || configFile.length() == 0) {
                SetupWizardDialog wizard = new SetupWizardDialog(null); // null = center on screen
                wizard.setVisible(true);
                if (!wizard.isSetupComplete()) {
                    System.exit(0);
                }
            }

            // Config exists — safe to construct MainFrame (panels will touch DBConnection)
            MainFrame frame = new MainFrame();

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
                DBConnection.getInstance().close()));

            LoginDialog login = new LoginDialog(frame);
            login.setVisible(true);

            // LoginDialog is modal — resumes here only on successful login.
            // If the user closes it, System.exit(0) already ran inside LoginDialog.
            frame.setVisible(true);
        });
    }
}