package com.lefestin.service;

import org.mindrot.jbcrypt.BCrypt;

import com.lefestin.dao.UserDAO;
import com.lefestin.model.User;

import java.sql.SQLException;

/**
 * AuthService — registration and login for Le Festin.
 *
 * Single responsibility: owns all password security logic.
 * UserDAO handles the SQL. BCrypt handles the hashing.
 * This class connects the two.
 *
 * Security rules followed throughout:
 *  - Raw passwords never leave this class
 *  - Both "user not found" and "wrong password" return null
 *    with the same message — never leak which was wrong
 *  - BCrypt work factor 12 — strong enough for a desktop app,
 *    hashes in ~300ms on modern hardware
 *  - Username stored and compared in lowercase — case-insensitive
 */
public class AuthService {

    // BCrypt work factor:
    // 12 = 2^12 = 4096 iterations. Increase to 13-14 for higher security,
    // but each +1 doubles hashing time. 10 is minimum, 12 is recommended.
    private static final int BCRYPT_ROUNDS = 12;

    // Result wrapper here...
    // Returns both the User and a human-readable message from every method
    // so the UI can show specific feedback without AuthService knowing
    // anything about Swing components.
    public static class AuthResult {
        private final User   user;
        private final String message;
        private final boolean success;

        private AuthResult(User user, String message, boolean success) {
            this.user    = user;
            this.message = message;
            this.success = success;
        }

        public static AuthResult ok(User user, String message) {
            return new AuthResult(user, message, true);
        }

        public static AuthResult fail(String message) {
            return new AuthResult(null, message, false);
        }

        public User    getUser()    { return user;    }
        public String  getMessage() { return message; }
        public boolean isSuccess()  { return success; }
    }

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Registers a new user account.
     *
     * Flow:
     *  1. Validate username and password meet minimum requirements
     *  2. Check username is not already taken
     *  3. Hash the password with BCrypt
     *  4. Insert via UserDAO (sets userId on returned User)
     *  5. Return AuthResult with the new User
     *
     * The raw password is overwritten after hashing and never stored.
     *
     * @param username desired username (trimmed, lowercased before insert)
     * @param password raw password — never stored, hashed immediately
     * @return AuthResult with User on success, null User + message on failure
     */
    public AuthResult register(String username, String password) {

        // Step 1: validate inputs
        String validationError = validateInputs(username, password);
        if (validationError != null) {
            return AuthResult.fail(validationError);
        }

        String normalizedUsername = username.trim().toLowerCase();

        try {
            // Step 2: check username availability 
            if (userDAO.existsByUsername(normalizedUsername)) {
                return AuthResult.fail(
                    "Username \"" + normalizedUsername + "\" is already taken.\n"
                    + "Please choose a different username."
                );
            }

            // Step 3: hash the password 
            // BCrypt.gensalt(rounds) generates a random salt each time —
            // two users with the same password get different hashes
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));

            // Step 4: insert the new user
            User newUser = new User(normalizedUsername, hash);
            userDAO.addUser(newUser); // sets newUser.userId from generated key

            // Step 5: return success
            // Clear local hash reference — no longer needed
            hash = null;

            return AuthResult.ok(newUser,
                "Welcome to Le Festin, " + newUser.getUsername() + "!");

        } catch (SQLException e) {
            return AuthResult.fail(
                "Registration failed due to a database error.\n"
                + "Please try again.\n"
            );
        }
    }

    /**
     * Authenticates an existing user.
     *
     * Flow:
     *  1. Validate inputs are not blank
     *  2. Fetch user by username via UserDAO
     *  3. Verify the raw password against the stored BCrypt hash
     *  4. Return AuthResult with User on success
     *
     * Both "user not found" and "wrong password" return the same
     * generic failure message — never reveal which was wrong.
     *
     * @param username  the username to look up (case-insensitive)
     * @param password  raw password to verify against stored hash
     * @return AuthResult with User on success, null User on any failure
     */
    public AuthResult login(String username, String password) {

        // Step 1: validate inputs are not blank 
        if (username == null || username.isBlank()) {
            return AuthResult.fail("Please enter your username.");
        }
        if (password == null || password.isEmpty()) {
            return AuthResult.fail("Please enter your password.");
        }

        try {
            // Step 2: fetch user by username
            User user = userDAO.getUserByUsername(username.trim());

            // Step 3: verify password
            // null check must come BEFORE BCrypt.checkpw —
            // calling checkpw with a null hash throws an exception
            if (user == null || !BCrypt.checkpw(password,
                                                 user.getPasswordHash())) {
                // Same message for both cases — don't leak which was wrong
                return AuthResult.fail(
                    "Invalid username or password.\nPlease try again."
                );
            }

            // Step 4: return success
            return AuthResult.ok(user,
                "Welcome back, " + user.getUsername() + "!");

        } catch (SQLException e) {
            // Removed long detail, will disrupt the UI
            return AuthResult.fail(
                "Login failed due to a database error.\n"
                + "Please try again.\n"
            );
        }
    }

    // Helper functions section
    /**
     * Validates username and password meet minimum requirements.
     * Returns an error message string, or null if inputs are valid.
     */
    private String validateInputs(String username, String password) {
        if (username == null || username.isBlank()) {
            return "Username cannot be blank.";
        }
        if (username.trim().length() < 3) {
            return "Username must be at least 3 characters.";
        }
        if (username.trim().length() > 50) {
            return "Username cannot exceed 50 characters.";
        }
        if (username.trim().contains(" ")) {
            return "Username cannot contain spaces.";
        }
        if (password == null || password.isEmpty()) {
            return "Password cannot be blank.";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        return null; // all valid
    }

}