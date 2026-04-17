package com.lafestin.model;

/**
 * User — maps directly to the `user` table.
 *
 * user(user_id, username, password_hash)
 *
 * Never expose passwordHash to the UI layer.
 * AuthService handles all BCrypt operations —
 * User.java is just a data carrier.
 */
public class User {

    // Fields
    private int    userId;
    private String username;
    private String passwordHash;

    public User(int userId, String username, String passwordHash) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
    }

    public User(String username, String passwordHash) {
        this(0, username, passwordHash);
    }

    public User() {}

    public int    getUserId()       { return userId;       }
    public String getUsername()     { return username;     }
    public String getPasswordHash() { return passwordHash; }

    public void setUserId(int userId)             { this.userId       = userId;       }
    public void setUsername(String username)       { this.username     = username;     }
    public void setPasswordHash(String hash)       { this.passwordHash = hash;         }

    @Override
    public String toString() {
        return "User{" +
            "userId="    + userId   +
            ", username='" + username + '\'' +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return this.userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
}