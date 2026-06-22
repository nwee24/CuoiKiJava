import java.sql.*;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/auction_db";
        String user = "postgres";
        String password = "123";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, status FROM products")) {
            
            System.out.println("Products in DB:");
            int count = 0;
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " - " + rs.getString("name") + " - " + rs.getString("status"));
                count++;
            }
            if (count == 0) System.out.println("NO PRODUCTS FOUND!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
