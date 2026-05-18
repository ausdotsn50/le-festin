package dao;

import java.sql.*;

import config.DBConnection;
import model.User;

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
public interface UserDAO {
    void addUser(User u);
    User getUserByUsername(String username);
    User getUserById(int userId);
    boolean existsByUsername(String username);
}