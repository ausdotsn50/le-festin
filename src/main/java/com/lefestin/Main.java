package com.lefestin;

import javax.swing.SwingUtilities;

import com.lefestin.config.DBConnection;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.LoginDialog;;

public class Main {
    public static void main(String[] args) {
        // Initializes overall app theme
        AppTheme.install(); 

        // Shut down the DB connection (upon Window close)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DBConnection.getInstance().close();
        }));

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            
            // Show login before making the main window visible
            LoginDialog login = new LoginDialog(frame);
            login.setVisible(true);

            // LoginDialog is modal — execution resumes here after
            // dispose() is called, which only happens on success.
            // If the user closed the dialog, System.exit(0) already ran.

            // Guarantees that ----- frame.getCurrentUser() is non-null!!!
            frame.setVisible(true);
        });

    }
}