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

public class JdbcHikariUtils {
    private static Properties jdbcProps = null;
    private static HikariDataSource dataSource;

    private JdbcHikariUtils(){
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

    private static synchronized void initPool() {
        if (dataSource != null) return;
        if(Objects.isNull(jdbcProps)){
            initAppConfig();
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcProps.getProperty("jdbc.url"));
        config.setUsername(jdbcProps.getProperty("jdbc.username"));
        config.setPassword(jdbcProps.getProperty("jdbc.password"));

        config.setMaximumPoolSize(100);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(3000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        System.out.println("HikariCP Connection Pool initialized!");
    }


    public static Connection getConnection() throws SQLException {
        if (dataSource == null){
            initPool();
            return dataSource.getConnection();
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
