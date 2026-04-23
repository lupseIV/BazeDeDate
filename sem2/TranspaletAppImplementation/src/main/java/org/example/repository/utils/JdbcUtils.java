package org.example.repository.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.TranspaletiiApp;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class JdbcUtils {

    private static Properties jdbcProps = null;
    private static Connection conn = null;

    private JdbcUtils(){
    }

    private static void initAppConfig() {
        jdbcProps = new Properties();
        try (InputStream is = TranspaletiiApp.class.getResourceAsStream("/config/db.config")) {
            if (is == null) {
                throw new RuntimeException("Cannot find db.config in the resources folder!");
            }
            jdbcProps.load(is);
            System.out.println("Properties loaded successfully!");
        } catch (Exception e) {
            System.out.println("Error loading config: " + e.getMessage());
        }
    }


    private static Connection createConnection(){
        if(Objects.isNull(jdbcProps)){
            initAppConfig();
        }

        String url = jdbcProps.getProperty("jdbc.url");
        String user = jdbcProps.getProperty("jdbc.username");
        String pass = jdbcProps.getProperty("jdbc.password");

        try {
            if (user != null && pass != null && !user.trim().isEmpty()) {
                return DriverManager.getConnection(url, user, pass);
            } else {
                return DriverManager.getConnection(url);
            }
        } catch (SQLException e) {
            throw new RuntimeException("CRITICAL: Failed to get database connection!", e);
        }
    }

    public static Connection getConnection(){
        if (conn == null){
            return createConnection();
        }
        return conn;
    }

    public static void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException("Error closing database connection", e);
            }
        }
    }
}