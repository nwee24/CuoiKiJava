package model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class Bid {
    private int id;
    private int sessionProductId;
    private int bidderId;
    private BigDecimal amount;
    private Timestamp bidTime;

    public Bid() {}

    public Bid(int id, int sessionProductId, int bidderId, BigDecimal amount, Timestamp bidTime) {
        this.id = id;
        this.sessionProductId = sessionProductId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionProductId() { return sessionProductId; }
    public void setSessionProductId(int sessionProductId) { this.sessionProductId = sessionProductId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
}
