package dao;

import model.AuctionSession;
import model.SessionProduct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class AuctionSessionDAO {
    // Tạo phiên đấu giá
    public boolean create(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (room_id, moderator_id, title, status, start_time, end_time, duration_seconds, fixed_fee, commission_percent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, session.getRoomId());
            ps.setInt(2, session.getModeratorId());
            ps.setString(3, session.getTitle());
            ps.setString(4, session.getStatus() != null ? session.getStatus() : "PREPARING");
            ps.setTimestamp(5, session.getStartTime());
            ps.setTimestamp(6, session.getEndTime());
            ps.setInt(7, session.getDurationSeconds());
            ps.setBigDecimal(8, session.getFixedFee());
            ps.setBigDecimal(9, session.getCommissionPercent());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) session.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Tìm phiên theo mã phòng (room_id)
    public AuctionSession findByRoomId(String roomId) {
        String sql = "SELECT * FROM auction_sessions WHERE room_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractSession(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE auction_sessions SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AuctionSession> findAll() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_sessions";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(extractSession(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Thêm sản phẩm vào phiên đấu giá
    public boolean addProduct(SessionProduct sp) {
        String sql = "INSERT INTO session_products (session_id, product_id, order_index, status, current_highest_bid) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sp.getSessionId());
            ps.setInt(2, sp.getProductId());
            ps.setInt(3, sp.getOrderIndex());
            ps.setString(4, sp.getStatus() != null ? sp.getStatus() : "WAITING");
            ps.setBigDecimal(5, sp.getCurrentHighestBid() != null ? sp.getCurrentHighestBid() : java.math.BigDecimal.ZERO);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) sp.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lấy sản phẩm tiếp theo để đấu giá trong phiên
    public SessionProduct getNextProduct(int sessionId) {
        String sql = "SELECT * FROM session_products WHERE session_id = ? AND status = 'WAITING' ORDER BY order_index ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractSessionProduct(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cập nhật người chiến thắng cho sản phẩm trong phiên
    public boolean updateWinner(int sessionProductId, int winnerId, BigDecimal finalPrice, String status) {
        String sql = "UPDATE session_products SET winner_id = ?, final_price = ?, status = ? WHERE id = ?";
        String sqlProduct = "UPDATE products SET status = 'SOLD' WHERE id = (SELECT product_id FROM session_products WHERE id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             PreparedStatement ps2 = conn.prepareStatement(sqlProduct)) {
            
            conn.setAutoCommit(false);
            ps.setInt(1, winnerId);
            ps.setBigDecimal(2, finalPrice);
            ps.setString(3, status);
            ps.setInt(4, sessionProductId);
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                if ("SOLD".equals(status)) {
                    ps2.setInt(1, sessionProductId);
                    ps2.executeUpdate();
                } else if ("PASSED".equals(status)) {
                    try (PreparedStatement ps3 = conn.prepareStatement(
                            "UPDATE products SET status = 'APPROVED' WHERE id = (SELECT product_id FROM session_products WHERE id = ?)")) {
                        ps3.setInt(1, sessionProductId);
                        ps3.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private AuctionSession extractSession(ResultSet rs) throws SQLException {
        AuctionSession a = new AuctionSession();
        a.setId(rs.getInt("id"));
        a.setRoomId(rs.getString("room_id"));
        a.setModeratorId(rs.getInt("moderator_id"));
        a.setTitle(rs.getString("title"));
        a.setStatus(rs.getString("status"));
        a.setStartTime(rs.getTimestamp("start_time"));
        a.setEndTime(rs.getTimestamp("end_time"));
        a.setDurationSeconds(rs.getInt("duration_seconds"));
        a.setExtensionCount(rs.getInt("extension_count"));
        a.setFixedFee(rs.getBigDecimal("fixed_fee"));
        a.setCommissionPercent(rs.getBigDecimal("commission_percent"));
        return a;
    }

    private SessionProduct extractSessionProduct(ResultSet rs) throws SQLException {
        SessionProduct sp = new SessionProduct();
        sp.setId(rs.getInt("id"));
        sp.setSessionId(rs.getInt("session_id"));
        sp.setProductId(rs.getInt("product_id"));
        sp.setOrderIndex(rs.getInt("order_index"));
        sp.setCurrentHighestBid(rs.getBigDecimal("current_highest_bid"));
        int wid = rs.getInt("winner_id");
        if (!rs.wasNull()) sp.setWinnerId(wid);
        sp.setFinalPrice(rs.getBigDecimal("final_price"));
        sp.setStatus(rs.getString("status"));
        sp.setTransactionStatus(rs.getString("transaction_status"));
        return sp;
    }
}
