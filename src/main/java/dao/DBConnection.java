package dao;

import shared.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Helper lấy connection từ cấu hình AppConfig
    public static Connection getConnection() throws SQLException {
        AppConfig config = AppConfig.getInstance();
        return DriverManager.getConnection(
            config.getDbUrl(),
            config.getDbUsername(),
            config.getDbPassword()
        );
    }
}
