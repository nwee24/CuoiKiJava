package dao;

import model.Bid;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
    // Lưu lịch sử đặt giá
    public boolean save(Bid bid) {
        String sql = "INSERT INTO bids (session_product_id, bidder_id, amount) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bid.getSessionProductId());
            ps.setInt(2, bid.getBidderId());
            ps.setBigDecimal(3, bid.getAmount());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) bid.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Bid> findBySessionProduct(int sessionProductId) {
        List<Bid> list = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE session_product_id = ? ORDER BY amount DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionProductId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(extractBid(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lấy lượt đấu giá cao nhất hiện tại cho sản phẩm
    public Bid getHighestBid(int sessionProductId) {
        String sql = "SELECT * FROM bids WHERE session_product_id = ? ORDER BY amount DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionProductId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractBid(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bid extractBid(ResultSet rs) throws SQLException {
        Bid b = new Bid();
        b.setId(rs.getInt("id"));
        b.setSessionProductId(rs.getInt("session_product_id"));
        b.setBidderId(rs.getInt("bidder_id"));
        b.setAmount(rs.getBigDecimal("amount"));
        b.setBidTime(rs.getTimestamp("bid_time"));
        return b;
    }
}
