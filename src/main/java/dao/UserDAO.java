package dao;

import model.User;
import model.Role;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // Đăng ký user mới với mật khẩu mã hóa BCrypt, kiểm tra trùng lặp username, email, phone
    public String register(String username, String password, Role role, String phone, String email, boolean isApproved) {
        // Kiểm tra trùng lặp trước
        String checkSql = "SELECT username, email, phone FROM users WHERE username = ? OR email = ? OR phone = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
             
            psCheck.setString(1, username);
            psCheck.setString(2, email);
            psCheck.setString(3, phone);
            
            try (ResultSet rs = psCheck.executeQuery()) {
                while (rs.next()) {
                    if (username.equalsIgnoreCase(rs.getString("username"))) {
                        return "Tên đăng nhập đã tồn tại!";
                    }
                    if (email.equalsIgnoreCase(rs.getString("email"))) {
                        return "Email đã được sử dụng!";
                    }
                    if (phone.equals(rs.getString("phone"))) {
                        return "Số điện thoại đã được sử dụng!";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Lỗi kiểm tra dữ liệu: " + e.getMessage();
        }

        // Insert nếu không trùng lặp
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password_hash, role, phone, email, is_approved) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, role.name());
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setBoolean(6, isApproved);
            ps.executeUpdate();
            return "SUCCESS";
        } catch (SQLException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    // Đăng nhập: lấy hash từ DB và verify với BCrypt (hỗ trợ login bằng Username, Email hoặc Phone)
    public User login(String usernameOrEmailOrPhone, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ? OR phone = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmailOrPhone);
            ps.setString(2, usernameOrEmailOrPhone);
            ps.setString(3, usernameOrEmailOrPhone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (BCrypt.checkpw(plainPassword, hash)) {
                    return extractUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Lấy User theo ID
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Lấy User theo Username
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cấm hoặc bỏ cấm User
    public boolean banUser(int id, boolean isBanned) {
        String sql = "UPDATE users SET is_banned = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isBanned);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Duyệt User
    public boolean approveUser(int id) {
        String sql = "UPDATE users SET is_approved = true WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cập nhật thông tin Mod (email, phone, password nếu có)
    public boolean updateModInfo(int id, String email, String phone, String plainPassword) {
        String sql;
        boolean updatePass = plainPassword != null && !plainPassword.trim().isEmpty();
        if (updatePass) {
            sql = "UPDATE users SET email = ?, phone = ?, password_hash = ? WHERE id = ?";
        } else {
            sql = "UPDATE users SET email = ?, phone = ? WHERE id = ?";
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);
            if (updatePass) {
                ps.setString(3, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
                ps.setInt(4, id);
            } else {
                ps.setInt(3, id);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cập nhật đánh giá
    public boolean updateRating(int id, BigDecimal newRating, int newCount) {
        String sql = "UPDATE users SET rating = ?, rating_count = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newRating);
            ps.setInt(2, newCount);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cập nhật số dư tài khoản
    public boolean updateBalance(int id, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lấy danh sách toàn bộ User
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(extractUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Hàm helper chuyển ResultSet thành object User
    private User extractUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setPhone(rs.getString("phone"));
        u.setEmail(rs.getString("email"));
        u.setRating(rs.getBigDecimal("rating"));
        u.setRatingCount(rs.getInt("rating_count"));
        u.setBalance(rs.getBigDecimal("balance"));
        u.setBanned(rs.getBoolean("is_banned"));
        u.setApproved(rs.getBoolean("is_approved"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
