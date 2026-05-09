package com.lefestin.config;

import java.nio.file.*;
import java.sql.*;

/**
 * SeedRunner — executes a .sql file against the la_festin database.
 *
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.lefestin.config.SeedRunner"
 *
 * By default runs: sql/la_festin_seed.sql
 * Edit SEED_FILE below to point to a different file.
 */
public class SeedRunner {

    // ── Change this to run a different seed file ───────────────────────
    private static final String SEED_FILE = "sql/la_festin_seed.sql";

    public static void main(String[] args) throws Exception {
        System.out.println("─────────────────────────────────────────");
        System.out.println("  Le Festin — Seed Runner                ");
        System.out.println("  File: " + SEED_FILE);
        System.out.println("─────────────────────────────────────────");

        // Read the entire .sql file
        String sql = Files.readString(Path.of(SEED_FILE));

        Connection conn =
            DBConnection.getInstance().getConnection();

        // Split on semicolons — each statement runs separately
        String[] statements = sql.split(";");

        int executed = 0;
        int skipped  = 0;

        for (String raw : statements) {
            String stmt = raw.strip();

            // Skip empty lines and comment-only blocks
            if (stmt.isEmpty()
                    || stmt.startsWith("--")
                    || stmt.replace("-", "")
                            .replace("\n", "")
                            .trim()
                            .isEmpty()) {
                skipped++;
                continue;
            }

            try (Statement s = conn.createStatement()) {
                s.execute(stmt);
                executed++;

                // Print SELECT results (e.g. the UNION verify query)
                ResultSet rs = s.getResultSet();
                if (rs != null) {
                    printResultSet(rs);
                }

            } catch (SQLException e) {
                System.err.println("✗ Failed on statement:");
                System.err.println("  " + stmt
                    .substring(0, Math.min(80, stmt.length()))
                    .replace("\n", " ") + "...");
                System.err.println("  SQLState:  " + e.getSQLState());
                System.err.println("  ErrorCode: " + e.getErrorCode());
                System.err.println("  Message:   " + e.getMessage());
                System.err.println();
            }
        }

        System.out.println();
        System.out.println("─────────────────────────────────────────");
        System.out.printf ("  Executed: %d statements%n", executed);
        System.out.printf ("  Skipped:  %d empty/comment blocks%n",
            skipped);
        System.out.println("─────────────────────────────────────────");

        // Fix passwords after seed
        System.out.println();
        System.out.println("Fixing BCrypt passwords...");
        fixPasswords(conn);

        DBConnection.getInstance().close();
        System.out.println("Done. Login with password: password123");
    }

    // ── Stamps real BCrypt hashes for all seeded users ─────────────────
    private static void fixPasswords(Connection conn)
            throws Exception {
        org.mindrot.jbcrypt.BCrypt.hashpw(
            "password123",
            org.mindrot.jbcrypt.BCrypt.gensalt(12)); // warm up

        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
            "password123",
            org.mindrot.jbcrypt.BCrypt.gensalt(12));

        String[] users = { "angela", "carl", "elizah" };

        for (String username : users) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE user SET password_hash = ? " +
                    "WHERE username = ?")) {
                stmt.setString(1, hash);
                stmt.setString(2, username);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("  ✓ " + username);
                } else {
                    System.out.println("  - " + username
                        + " not found, skipped");
                }
            }
        }
    }

    // ── Prints a ResultSet as a simple table ───────────────────────────
    private static void printResultSet(ResultSet rs)
            throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        // Print header
        StringBuilder header = new StringBuilder("  ");
        for (int i = 1; i <= cols; i++) {
            header.append(String.format("%-22s", 
                meta.getColumnLabel(i)));
        }
        System.out.println(header);
        System.out.println("  " + "─".repeat(cols * 22));

        // Print rows
        while (rs.next()) {
            StringBuilder row = new StringBuilder("  ");
            for (int i = 1; i <= cols; i++) {
                row.append(String.format("%-22s",
                    rs.getString(i)));
            }
            System.out.println(row);
        }
        System.out.println();
    }
}