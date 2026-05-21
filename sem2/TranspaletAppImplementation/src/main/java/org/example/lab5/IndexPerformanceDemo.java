package org.example.lab5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.TranspaletiiApp;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class IndexPerformanceDemo {

    private static final Logger logger = LogManager.getLogger(IndexPerformanceDemo.class);
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 50;

    public static void main(String[] args) {
        Properties props = loadConfig();
        if (props == null) return;

        try (Connection conn = DriverManager.getConnection(
                props.getProperty("jdbc.url"),
                props.getProperty("jdbc.username"),
                props.getProperty("jdbc.password"))) {

            System.out.println("=== LAB 5 - INDEX PERFORMANCE MEASUREMENT ===\n");

            measureSerialNumberSearch(conn);
            System.out.println();
            measureRentalJoinByTruckId(conn);

        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: Search PalletTrucks by serial_number
    //         Index: idx_pallettrucks_serial_number
    // -------------------------------------------------------------------------
    private static void measureSerialNumberSearch(Connection conn) throws SQLException {
        System.out.println("--- Test 1: PalletTrucks lookup by serial_number ---");

        // Force FULL TABLE SCAN (no index)
        String scanQuery = "SELECT * FROM PalletTrucks WITH (INDEX(0)) WHERE serial_number LIKE 'SN%'";
        // Use the index normally
        String indexQuery = "SELECT * FROM PalletTrucks WHERE serial_number LIKE 'SN%'";

        double scanAvg  = benchmark(conn, scanQuery,  "Full scan");
        double indexAvg = benchmark(conn, indexQuery, "With index (idx_pallettrucks_serial_number)");

        printSummary(scanAvg, indexAvg);
    }


    private static void measureRentalJoinByTruckId(Connection conn) throws SQLException {
        System.out.println("--- Test 2: Rentals JOIN PalletTrucks by truck_id ---");

        String scanQuery  = "SELECT r.rental_id, t.serial_number, r.return_status "
                + "FROM Rentals r WITH (INDEX(0)) "
                + "JOIN PalletTrucks t ON r.truck_id = t.truck_id "
                + "WHERE r.return_status = 'Active'";

        String indexQuery = "SELECT r.rental_id, t.serial_number, r.return_status "
                + "FROM Rentals r "
                + "JOIN PalletTrucks t ON r.truck_id = t.truck_id "
                + "WHERE r.return_status = 'Active'";

        double scanAvg  = benchmark(conn, scanQuery,  "Full scan");
        double indexAvg = benchmark(conn, indexQuery, "With index (idx_rentals_truck_id)");

        printSummary(scanAvg, indexAvg);
    }


    private static double benchmark(Connection conn, String sql, String label) throws SQLException {

        for (int i = 0; i < WARMUP_RUNS; i++) {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { }
            }
        }

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {  }
            }
            times.add(System.nanoTime() - start);
        }

        double avgMs = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        System.out.printf("  %-48s  avg = %.3f ms%n", label + ":", avgMs);
        return avgMs;
    }

    private static void printSummary(double scanAvg, double indexAvg) {
        if (indexAvg > 0 && scanAvg > indexAvg) {
            System.out.printf("  => Index is %.1fx faster than full scan%n", scanAvg / indexAvg);
        } else {
            System.out.println("  => Dataset too small to show speedup (index overhead)");
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = TranspaletiiApp.class.getResourceAsStream("/config/db.config")) {
            if (is == null) {
                logger.error("Cannot find /config/db.config");
                return null;
            }
            props.load(is);
            return props;
        } catch (Exception e) {
            logger.error("Error loading config: {}", e.getMessage());
            return null;
        }
    }
}
