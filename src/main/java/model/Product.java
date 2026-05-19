package model;

import java.math.BigDecimal;

public class Product {
    private int id;
    private int sellerId;
    private String name;
    private String description;
    private String imageData;
    private BigDecimal startingPrice;
    private String status;

    public Product() {}

    public Product(int id, int sellerId, String name, String description, String imageData, BigDecimal startingPrice, String status) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.imageData = imageData;
        this.startingPrice = startingPrice;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
