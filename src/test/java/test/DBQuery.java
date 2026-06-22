package test;
import dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBQuery {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, product_id, winner_id, final_price, status, transaction_status FROM session_products")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.printf("sp_id: %d, prod_id: %d, winner_id: %d, status: %s, tx_status: %s\n",
                    rs.getInt("id"), rs.getInt("product_id"), rs.getInt("winner_id"),
                    rs.getString("status"), rs.getString("transaction_status"));
            }
            System.out.println("---");
            PreparedStatement ps2 = conn.prepareStatement("SELECT id, name, status FROM products");
            ResultSet rs2 = ps2.executeQuery();
            while(rs2.next()) {
                System.out.printf("prod_id: %d, name: %s, status: %s\n", rs2.getInt("id"), rs2.getString("name"), rs2.getString("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
