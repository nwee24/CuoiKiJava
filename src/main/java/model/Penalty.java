package model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class Penalty {
    private int id;
    private int userId;
    private Integer sessionProductId;
    private BigDecimal amount;
    private String reason;
    private Timestamp createdAt;

    public Penalty() {}

    public Penalty(int id, int userId, Integer sessionProductId, BigDecimal amount, String reason, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.sessionProductId = sessionProductId;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getSessionProductId() { return sessionProductId; }
    public void setSessionProductId(Integer sessionProductId) { this.sessionProductId = sessionProductId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
