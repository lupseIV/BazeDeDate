package org.example.transpaletiiapp.repository.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcUtils {

    private Properties jdbcProps;

    public JdbcUtils(Properties props){
        jdbcProps = props;
    }

    // We removed the 'private Connection instance' variable!
    // Every repository method gets its own isolated connection so they don't close each other's.

    public Connection getConnection(){
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

    public Properties getJdbcProps() {
        return jdbcProps;
    }
}