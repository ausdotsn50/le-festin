package com.lafestin;

import javax.swing.SwingUtilities;

import com.lafestin.config.DBConnection;
import com.lafestin.model.User;
import com.lafestin.ui.MainFrame;

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

            // Temporary: hardcode angela until LoginDialog is built
            frame.setCurrentUser(new User(1, "angela", ""));

            frame.setVisible(true);
        });
    }
}