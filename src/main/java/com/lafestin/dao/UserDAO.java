package com.lafestin.dao;

import com.lafestin.config.DBConnection;
import com.lafestin.model.User;

import java.sql.*;

/**
 * UserDAO — all SQL for the `user` table.
 *
 * user(user_id PK, username UNIQUE, password_hash)
 *
 * Security rules followed throughout:
 *  - password_hash is never logged or printed
 *  - getUserByUsername returns null on miss — never throws
 *    so AuthService can distinguish "wrong password" from
 *    "user does not exist" cleanly
 *  - Raw password strings never touch this class —
 *    hashing and verification belong in AuthService
 */
public class UserDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts a new user row.
     * Sets userId on the User object from the generated key.
     *
     * The password stored here must already be a BCrypt hash —
     * never pass a raw password to this method.
     * That hashing is done in AuthService.register() before
     * this method is called.
     *
     * Throws if username already exists — username has a UNIQUE
     * constraint. AuthService should call existsByUsername()
     * first and show a friendly error before reaching here.
     *
     * @param u  User with userId=0 and passwordHash already set
     * @throws SQLException if INSERT fails or username taken (code 1062)
     */
    public void addUser(User u) throws SQLException {
        String sql = """
            INSERT INTO user (username, password_hash)
            VALUES (?, ?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, u.getUsername().trim().toLowerCase());
            stmt.setString(2, u.getPasswordHash());

            stmt.executeUpdate();

            // Write the DB-assigned id back onto the model object
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    u.setUserId(keys.getInt(1));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Looks up a user by username — the primary login lookup.
     *
     * Returns null if no row found. Never throws on a missing user.
     * AuthService.login() uses the null return to detect an unknown
     * username and return a generic "invalid credentials" message
     * without leaking which field was wrong.
     *
     * Username comparison is case-insensitive — stored as lowercase,
     * queried via LOWER() so "Angela", "angela", "ANGELA" all match.
     *
     * @param username  the username to look up (any case)
     * @return the User with passwordHash set, or null if not found
     * @throws SQLException if the SELECT itself fails (connection lost etc.)
     */
    public User getUserByUsername(String username) throws SQLException {
        String sql = """
            SELECT user_id, username, password_hash
            FROM user
            WHERE LOWER(username) = LOWER(?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null; // user not found — not an error
    }

    /**
     * Looks up a user by primary key.
     * Used after login to refresh the session user object,
     * and by MainFrame.setCurrentUser() on app startup.
     *
     * Returns null if not found — same convention as getUserByUsername.
     *
     * @param userId  the user_id to look up
     * @return the User, or null if not found
     * @throws SQLException if the SELECT fails
     */
    public User getUserById(int userId) throws SQLException {
        String sql = """
            SELECT user_id, username, password_hash
            FROM user
            WHERE user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    /**
     * Checks whether a username is already taken.
     * Call this in AuthService.register() before addUser()
     * to show a friendly "username already exists" message
     * rather than letting a raw SQLException bubble up.
     *
     * @param username  username to check (any case)
     * @return true if already taken
     * @throws SQLException if SELECT fails
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM user
            WHERE LOWER(username) = LOWER(?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Maps one ResultSet row to a User.
     * passwordHash is included — AuthService needs it for BCrypt verify.
     * It is never printed — User.toString() deliberately omits it.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt(   "user_id"),
            rs.getString("username"),
            rs.getString("password_hash")
        );
    }
}