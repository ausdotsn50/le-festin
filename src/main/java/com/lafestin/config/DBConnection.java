package com.lafestin.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — JDBC singleton for La Festin.
 *
 * Usage anywhere in the project:
 *   Connection conn = DBConnection.getInstance().getConnection();
 */
public class DBConnection {

    // DB conn instance
    private static DBConnection instance;
    
    // Live JDBC conn
    private Connection connection;

    // config.properties
    private static final String URL = ConfigLoader.get("db.url");
    private static final String USER = ConfigLoader.get("db.user");
    private static final String PASSWORD = ConfigLoader.get("db.password");

    // Introduced separation on DBConn and config loading
    private DBConnection() {
        connect();
    }

    private void connect() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DBConnection] Connected to MySQL at: " + URL);
        } catch (SQLException e) {
            throw new RuntimeException(
                "[DBConnection] Failed to connect to MySQL.\n" +
                "  → Is MySQL running? Check XAMPP or your local MySQL service.\n" +
                "  → URL: " + URL + "\n" +
                "  → User: " + USER, e
            );
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || !connection.isValid(2)) {
                System.out.println("[DBConnection] Connection stale — reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("[DBConnection] Validation failed — reconnecting...");
            connect();
        }
        return connection;
    }

    // Call on APP SHUTDOWN
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DBConnection] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }
}