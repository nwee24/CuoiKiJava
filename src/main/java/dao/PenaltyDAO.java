package dao;

import model.Penalty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PenaltyDAO {
    // Lưu lịch sử phạt
    public boolean save(Penalty penalty) {
        String sql = "INSERT INTO penalties (user_id, session_product_id, amount, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, penalty.getUserId());
            
            if (penalty.getSessionProductId() != null) ps.setInt(2, penalty.getSessionProductId());
            else ps.setNull(2, Types.INTEGER);
            
            ps.setBigDecimal(3, penalty.getAmount());
            ps.setString(4, penalty.getReason());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) penalty.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lấy danh sách bị phạt của một user
    public List<Penalty> findByUser(int userId) {
        List<Penalty> list = new ArrayList<>();
        String sql = "SELECT * FROM penalties WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Penalty p = new Penalty();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                
                int spid = rs.getInt("session_product_id");
                if (!rs.wasNull()) p.setSessionProductId(spid);
                
                p.setAmount(rs.getBigDecimal("amount"));
                p.setReason(rs.getString("reason"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
