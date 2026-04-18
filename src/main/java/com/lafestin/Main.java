package com.lafestin;

import javax.swing.SwingUtilities;

import com.lafestin.config.DBConnection;
import com.lafestin.ui.MainFrame;
import com.lafestin.ui.dialogs.LoginDialog;
import com.lafestin.ui.AppTheme;;

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