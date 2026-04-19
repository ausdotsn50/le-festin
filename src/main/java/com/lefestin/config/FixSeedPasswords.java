package com.lefestin.config;

import org.mindrot.jbcrypt.BCrypt;

import com.lefestin.dao.UserDAO;
import com.lefestin.model.User;

import java.sql.*;

public class FixSeedPasswords {

    public static void main(String[] args) throws Exception {
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(12));

        UserDAO dao = new UserDAO();
        Connection conn = DBConnection.getInstance().getConnection();

        for (String name : new String[]{"angela", "carl", "elizah"}) {
            User u = dao.getUserByUsername(name);
            if (u != null) {
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE user SET password_hash = ? WHERE user_id = ?");
                stmt.setString(1, hash);
                stmt.setInt(   2, u.getUserId());
                stmt.executeUpdate();
                stmt.close();
                System.out.println("Fixed: " + name);
            } else {
                System.out.println("Not found: " + name);
            }
        }

        DBConnection.getInstance().close();
        System.out.println("Done — all seed passwords set to: password123");
    }
}
