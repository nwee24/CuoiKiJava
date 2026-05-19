package model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class AuctionSession {
    private int id;
    private String roomId;
    private int moderatorId;
    private String title;
    private String status;
    private Timestamp startTime;
    private Timestamp endTime;
    private int durationSeconds;
    private int extensionCount;
    private BigDecimal fixedFee;
    private BigDecimal commissionPercent;

    public AuctionSession() {}

    public AuctionSession(int id, String roomId, int moderatorId, String title, String status, Timestamp startTime, Timestamp endTime, int durationSeconds, int extensionCount, BigDecimal fixedFee, BigDecimal commissionPercent) {
        this.id = id;
        this.roomId = roomId;
        this.moderatorId = moderatorId;
        this.title = title;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.extensionCount = extensionCount;
        this.fixedFee = fixedFee;
        this.commissionPercent = commissionPercent;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public int getModeratorId() { return moderatorId; }
    public void setModeratorId(int moderatorId) { this.moderatorId = moderatorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getExtensionCount() { return extensionCount; }
    public void setExtensionCount(int extensionCount) { this.extensionCount = extensionCount; }

    public BigDecimal getFixedFee() { return fixedFee; }
    public void setFixedFee(BigDecimal fixedFee) { this.fixedFee = fixedFee; }

    public BigDecimal getCommissionPercent() { return commissionPercent; }
    public void setCommissionPercent(BigDecimal commissionPercent) { this.commissionPercent = commissionPercent; }
}
