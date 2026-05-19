package model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private BigDecimal rating;
    private int ratingCount;
    private BigDecimal balance;
    private boolean isBanned;
    private Timestamp createdAt;

    public User() {}

    public User(int id, String username, String passwordHash, Role role, BigDecimal rating, int ratingCount, BigDecimal balance, boolean isBanned, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.balance = balance;
        this.isBanned = isBanned;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean isBanned) { this.isBanned = isBanned; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
