package com.lafestin.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ConnectionTest — one-shot smoke test for the JDBC connection.
 * Run this directly before writing any DAO code.
 * Delete or comment out after confirming the connection works.
 */
public class ConnectionTest {

    public static void main(String[] args) {
        System.out.println("─────────────────────────────────────");
        System.out.println("  La Festin — JDBC Connection Test   ");
        System.out.println("─────────────────────────────────────");

        try {
            // Get the connection through the singleton
            Connection conn = DBConnection.getInstance().getConnection();
            System.out.println("✓ DBConnection.getInstance() — OK");

            // Run SELECT 1 — the lightest possible query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 AS result");

            if (rs.next() && rs.getInt("result") == 1) {
                System.out.println("✓ SELECT 1 returned: " + rs.getInt("result") + " — MySQL is responding");
            }

            // Print the actual DB we landed on — confirms correct schema
            ResultSet dbRs = stmt.executeQuery("SELECT DATABASE() AS db");
            if (dbRs.next()) {
                System.out.println("✓ Connected to database: " + dbRs.getString("db"));
            }

            // Print MySQL server version
            ResultSet vRs = stmt.executeQuery("SELECT VERSION() AS version");
            if (vRs.next()) {
                System.out.println("✓ MySQL version: " + vRs.getString("version"));
            }

            System.out.println("─────────────────────────────────────");
            System.out.println("  ALL CHECKS PASSED — ready to build ");
            System.out.println("─────────────────────────────────────");

            stmt.close();
            DBConnection.getInstance().close();

        } catch (Exception e) {
            System.err.println("─────────────────────────────────────");
            System.err.println("  CONNECTION FAILED                  ");
            System.err.println("─────────────────────────────────────");
            System.err.println("Reason: " + e.getMessage());
            System.err.println();
            System.err.println("Checklist:");
            System.err.println("  [ ] MySQL is running (check XAMPP Control Panel)");
            System.err.println("  [ ] Database 'la_festin' exists (run your schema script first)");
            System.err.println("  [ ] db.user and db.password are correct in config.properties");
            System.err.println("  [ ] db.url is jdbc:mysql://localhost:3306/la_festin");
        }
    }
}