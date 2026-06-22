package dao;

import model.Rating;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public boolean addRating(Rating rating) {
        String insertSql = "INSERT INTO ratings (rater_id, rated_id, session_id, score, comment) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE users SET rating_count = rating_count + 1, " +
                           "rating = ((rating * rating_count) + ?) / (rating_count + 1) WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                 
                ps1.setInt(1, rating.getRaterId());
                ps1.setInt(2, rating.getRatedId());
                if (rating.getSessionId() != null) {
                    ps1.setInt(3, rating.getSessionId());
                } else {
                    ps1.setNull(3, Types.INTEGER);
                }
                ps1.setInt(4, rating.getScore());
                ps1.setString(5, rating.getComment());
                
                int affected = ps1.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps1.getGeneratedKeys()) {
                        if (rs.next()) rating.setId(rs.getInt(1));
                    }
                    
                    ps2.setInt(1, rating.getScore());
                    ps2.setInt(2, rating.getRatedId());
                    ps2.executeUpdate();
                    
                    conn.commit();
                    return true;
                }
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public List<Rating> findByRatedId(int ratedId) {
        List<Rating> list = new ArrayList<>();
        String sql = "SELECT * FROM ratings WHERE rated_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ratedId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rating r = new Rating();
                    r.setId(rs.getInt("id"));
                    r.setRaterId(rs.getInt("rater_id"));
                    r.setRatedId(rs.getInt("rated_id"));
                    r.setSessionId(rs.getObject("session_id") != null ? rs.getInt("session_id") : null);
                    r.setScore(rs.getInt("score"));
                    r.setComment(rs.getString("comment"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
