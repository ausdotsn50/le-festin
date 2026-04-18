package com.lafestin;

import java.sql.SQLException;
import java.util.List;

import javax.swing.SwingUtilities;

import com.lafestin.config.DBConnection;
import com.lafestin.dao.RecipeDAO;
import com.lafestin.model.Recipe;
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