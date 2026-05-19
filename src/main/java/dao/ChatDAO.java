package dao;

import model.ChatMessage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {
    // Lưu tin nhắn mới vào CSDL
    public boolean saveMessage(ChatMessage msg) {
        String sql = "INSERT INTO chat_messages (room_id, sender_id, receiver_id, content, message_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, msg.getRoomId());
            if (msg.getSenderId() != null) ps.setInt(2, msg.getSenderId());
            else ps.setNull(2, Types.INTEGER);
            
            if (msg.getReceiverId() != null) ps.setInt(3, msg.getReceiverId());
            else ps.setNull(3, Types.INTEGER);
            
            ps.setString(4, msg.getContent());
            ps.setString(5, msg.getMessageType());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) msg.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lịch sử chat riêng giữa 2 user
    public List<ChatMessage> getPrivateHistory(int user1, int user2) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE message_type = 'PRIVATE' AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) ORDER BY sent_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user1);
            ps.setInt(2, user2);
            ps.setInt(3, user2);
            ps.setInt(4, user1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(extractMsg(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lịch sử chat của phòng (public)
    public List<ChatMessage> getRoomHistory(String roomId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE room_id = ? AND message_type != 'PRIVATE' ORDER BY sent_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(extractMsg(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ChatMessage extractMsg(ResultSet rs) throws SQLException {
        ChatMessage msg = new ChatMessage();
        msg.setId(rs.getInt("id"));
        msg.setRoomId(rs.getString("room_id"));
        
        int sid = rs.getInt("sender_id");
        if (!rs.wasNull()) msg.setSenderId(sid);
        
        int rid = rs.getInt("receiver_id");
        if (!rs.wasNull()) msg.setReceiverId(rid);
        
        msg.setContent(rs.getString("content"));
        msg.setSentAt(rs.getTimestamp("sent_at"));
        msg.setMessageType(rs.getString("message_type"));
        return msg;
    }
}
