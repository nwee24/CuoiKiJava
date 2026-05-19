package model;

import java.math.BigDecimal;

public class SessionProduct {
    private int id;
    private int sessionId;
    private int productId;
    private int orderIndex;
    private BigDecimal currentHighestBid;
    private Integer winnerId;
    private BigDecimal finalPrice;
    private String status;

    public SessionProduct() {}

    public SessionProduct(int id, int sessionId, int productId, int orderIndex, BigDecimal currentHighestBid, Integer winnerId, BigDecimal finalPrice, String status) {
        this.id = id;
        this.sessionId = sessionId;
        this.productId = productId;
        this.orderIndex = orderIndex;
        this.currentHighestBid = currentHighestBid;
        this.winnerId = winnerId;
        this.finalPrice = finalPrice;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
