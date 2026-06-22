package model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private String phone;
    private String email;
    private BigDecimal rating;
    private int ratingCount;
    private BigDecimal balance;
    private boolean isBanned;
    private boolean isApproved;
    private Timestamp createdAt;

    public User() {}

    public User(int id, String username, String passwordHash, Role role, String phone, String email, BigDecimal rating, int ratingCount, BigDecimal balance, boolean isBanned, boolean isApproved, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.balance = balance;
        this.isBanned = isBanned;
        this.isApproved = isApproved;
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

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean isBanned) { this.isBanned = isBanned; }
    
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean isApproved) { this.isApproved = isApproved; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
