import dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestQuery {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM session_products LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("SUCCESS. Column count: " + rs.getMetaData().getColumnCount());
                for(int i=1;i<=rs.getMetaData().getColumnCount();i++)
                    System.out.print(rs.getMetaData().getColumnName(i) + ", ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
