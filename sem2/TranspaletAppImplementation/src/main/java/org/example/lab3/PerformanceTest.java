package org.example.lab3;

import org.example.TranspaletiiApp;
import org.example.repository.utils.JdbcHikariUtils;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PerformanceTest {
    private static Properties jdbcProps = null;
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
    public static void main(String[] args) {
//        initAppConfig();
//        testNoPooling(100);
//        testPooling(100);
        testNoConnectionClose(200);
        JdbcHikariUtils.closePool();
        testNoConnectionCloseFixed(200);
    }

    private static void testNoPooling(int testSize){
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Double> connTimes = new ArrayList<>();
        for (int i = 0; i < testSize; i++) {
            long startConn = System.nanoTime();
            try (
                Connection connection =  DriverManager.getConnection(
                        jdbcProps.getProperty("jdbc.url"),
                        jdbcProps.getProperty("jdbc.username"),
                        jdbcProps.getProperty("jdbc.password"))){
                long endConn = System.nanoTime();
                var stmt = connection.createStatement();
                stmt.execute("SELECT 1");

                connTimes.add((endConn - startConn) / 1_000_000.0);
            } catch (SQLException e) {
                throw new RuntimeException("CRITICAL: Failed to get database connection!", e);
            }
        }
        double averageConnTime = connTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        System.out.println("Average connection time without pooling: " + averageConnTime + " ms");
        Timestamp after = new Timestamp(System.currentTimeMillis());
        System.out.println("Time taken without pooling: " + (after.getTime() - now.getTime()) + " ms");
    }

    private static void testPooling(int testSize){
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Double> connTimes = new ArrayList<>();

        for (int i = 0; i < testSize; i++) {
            long startConn = System.nanoTime();
            try (
                    Connection connection = JdbcHikariUtils.getConnection();) {

                long endConn = System.nanoTime();
                var stmt = connection.createStatement();
                stmt.execute("SELECT 1");

                connTimes.add((endConn - startConn) / 1_000_000.0);
            } catch (SQLException e) {
                throw new RuntimeException("CRITICAL: Failed to get database connection!", e);
            }



        }
        double averageConnTime = connTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        System.out.println("Average connection time with pooling: " + averageConnTime + " ms");
        Timestamp after = new Timestamp(System.currentTimeMillis());
        System.out.println("Time taken with pooling: " + (after.getTime() - now.getTime()) + " ms");
    }

    private static void testNoConnectionClose(int testSize){

        for (int i = 0; i < testSize; i++) {
            System.out.println("Iteration " + (i) + ": Attempting to get connection...");

            try {
                Connection connection = JdbcHikariUtils.getConnection();

                var stmt = connection.createStatement();
                stmt.execute("SELECT 1");

            } catch (SQLTransientConnectionException e) {
                System.out.println("Connection pool exhausted after " + i + " iterations.");
                break;
            }catch (SQLException e) {
                throw new RuntimeException("Failed to get database connection!", e);
            } finally {
                System.out.println("Keeping connection " + i);

            }

        }
    }

    private static void testNoConnectionCloseFixed(int testSize){
        for (int i = 0; i < testSize; i++) {
            System.out.println("Iteration " + (i) + ": Attempting to get connection...");

            try
                (Connection connection = JdbcHikariUtils.getConnection();){

                var stmt = connection.createStatement();
                stmt.execute("SELECT 1");

            } catch (SQLTransientConnectionException e) {
                System.out.println("Connection pool exhausted after " + i + " iterations.");
                break;
            }catch (SQLException e) {
                throw new RuntimeException("Failed to get database connection!", e);
            }finally {
                System.out.println("Closed connection " + i);
            }

        }
    }

}
