package model;

import java.sql.Timestamp;

public class ChatMessage {
    private int id;
    private String roomId;
    private Integer senderId;
    private Integer receiverId;
    private String content;
    private Timestamp sentAt;
    private String messageType;

    public ChatMessage() {}

    public ChatMessage(int id, String roomId, Integer senderId, Integer receiverId, String content, Timestamp sentAt, String messageType) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = sentAt;
        this.messageType = messageType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }

    public Integer getReceiverId() { return receiverId; }
    public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
