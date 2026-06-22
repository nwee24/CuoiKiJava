package dao;

import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

public class AdminDAO {
    
    // Đếm tổng số phiên đấu giá
    public int getTotalSessions() {
        String sql = "SELECT COUNT(*) FROM auction_sessions";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Tính tổng doanh thu (Chỉ tính giao dịch type = COMMISSION)
    public BigDecimal getTotalCommission() {
        String sql = "SELECT SUM(amount) FROM transactions WHERE type = 'COMMISSION'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    // Lấy doanh thu theo bộ lọc: DAY, WEEK, MONTH, YEAR
    public String getRevenueData(String filter) {
        String selectFormat = "";
        String groupFormat = "";
        String orderFormat = "";
        int limit = 7;
        String prefix = "";
        
        switch (filter) {
            case "DAY":
                selectFormat = "DD/MM";
                groupFormat = "DD/MM";
                orderFormat = "YYYYMMDD";
                limit = 14; // Lấy 14 ngày
                break;
            case "WEEK":
                selectFormat = "IW";
                groupFormat = "IW";
                orderFormat = "IYYYIW";
                limit = 12; // Lấy 12 tuần
                prefix = "Tuần ";
                break;
            case "YEAR":
                selectFormat = "YYYY";
                groupFormat = "YYYY";
                orderFormat = "YYYY";
                limit = 5; // Lấy 5 năm
                break;
            case "MONTH":
            default:
                selectFormat = "MM/YYYY";
                groupFormat = "MM/YYYY";
                orderFormat = "YYYYMM";
                limit = 12; // Lấy 12 tháng
                break;
        }

        String sql = "SELECT TO_CHAR(created_at, '" + selectFormat + "') as label, SUM(amount) as total " +
                     "FROM transactions WHERE type = 'COMMISSION' " +
                     "GROUP BY TO_CHAR(created_at, '" + groupFormat + "'), TO_CHAR(created_at, '" + orderFormat + "') " +
                     "ORDER BY TO_CHAR(created_at, '" + orderFormat + "') DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            
            ResultSet rs = ps.executeQuery();
            List<String> data = new ArrayList<>();
            while (rs.next()) {
                String label = rs.getString("label");
                BigDecimal total = rs.getBigDecimal("total");
                if (total == null) total = BigDecimal.ZERO;
                int val = total.intValue();
                
                // Nếu là tháng (MM/YYYY), ta có thể rút gọn thành T.MM hoặc giữ nguyên
                if (filter.equals("MONTH")) {
                    String[] parts = label.split("/");
                    label = "T" + Integer.parseInt(parts[0]);
                }
                
                data.add(prefix + label + ":" + val);
            }
            // Đảo ngược để vẽ biểu đồ từ cũ -> mới
            Collections.reverse(data);
            for (String s : data) {
                if (sb.length() > 0) sb.append("|");
                sb.append(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    // Lấy toàn bộ danh sách phạt để Admin xem (đã join Username)
    // Format trả về: id,username,reason,amount,created_at|...
    public String getAllPenaltiesWithUsername() {
        String sql = "SELECT p.id, u.username, p.reason, p.amount, p.created_at " +
                     "FROM penalties p JOIN users u ON p.user_id = u.id " +
                     "ORDER BY p.created_at DESC";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (sb.length() > 0) sb.append("|");
                String reason = rs.getString("reason");
                if (reason == null) reason = "";
                sb.append(rs.getInt("id")).append(",")
                  .append(rs.getString("username")).append(",")
                  .append(reason.replace(",", ";")).append(",")
                  .append(rs.getBigDecimal("amount").intValue()).append(",")
                  .append(rs.getTimestamp("created_at").toString().substring(0, 10)); // YYYY-MM-DD
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
