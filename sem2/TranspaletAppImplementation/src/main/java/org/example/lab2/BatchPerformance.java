package org.example.lab2;

import org.example.repository.utils.JdbcUtils;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

class BatchPerformance {

    private static final int RECORDS_COUNT = 5000;

    private void cleanUp() {
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM Employees");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void runPerformanceTests() {
        System.out.println("\n=== 3. Testare Performanta Inserare 5000 inregistrari ===");

        long timeAutoCommit = 0;
        long timeBatchCommit = 0;
        long timeExecuteBatch = 0;

        int runs = 3;
        List<Long> t1 = new ArrayList<>();
        List<Long> t2 = new ArrayList<>();
        List<Long> t3 = new ArrayList<>();
        for (int i = 1; i <= runs; i++) {
            System.out.println("\n--- Rularea " + i + " ---");

            cleanUp();
            t1.add( testAutoCommit());
            System.out.println("Abordarea 1 (Auto-commit): " + t1.getLast() + " ms");
            timeAutoCommit += t1.getLast();

            cleanUp();
            t2.add(testCommitInLots());
            System.out.println("Abordarea 2 (Commit la fiecare 100): " + t2.getLast() + " ms");
            timeBatchCommit += t2.getLast();

            cleanUp();
            t3.add(testSingleTransactionBatch());
            System.out.println("Abordarea 3 (ExecuteBatch la fiecare 50): " + t3.getLast() + " ms");
            timeExecuteBatch += t3.getLast();
        }

        System.out.println("\n=== Rezultate Medii (" + runs + " rulari) ===");
        System.out.println("1. Auto-commit: " + (timeAutoCommit / runs) + " ms");
        System.out.println("2. Commit in loturi (100): " + (timeBatchCommit / runs) + " ms");
        System.out.println("3. Tranzactie Unica (executeBatch): " + (timeExecuteBatch / runs) + " ms");
        cleanUp();
        saveToCsvAndPlot(t1,t2,t3,runs);
    }

    private long testAutoCommit() {
        long startTime = System.currentTimeMillis();
        try (Connection conn = JdbcUtils.getConnection()) {
            String sql = "INSERT INTO Employees (first_name, last_name, role, hire_date, phone, email) VALUES (?, ?, ?,?,?,?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORDS_COUNT; i++) {
                    stmt.setString(1, "FirstName_" + i);
                    stmt.setString(2, "LastName_" + i);
                    stmt.setString(3, "Sales");
                    stmt.setDate(4, Date.valueOf(LocalDate.now()));
                    stmt.setString(5, "123456789");
                    stmt.setString(6, "employee" + i + "@example.com");
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return System.currentTimeMillis() - startTime;
    }

    private long testCommitInLots() {
        long startTime = System.currentTimeMillis();
        try (Connection conn = JdbcUtils.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO Employees (first_name, last_name, role, hire_date, phone, email) VALUES (?, ?, ?,?,?,?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORDS_COUNT; i++) {
                    stmt.setString(1, "FirstName_" + i);
                    stmt.setString(2, "LastName_" + i);
                    stmt.setString(3, "Sales");
                    stmt.setDate(4, Date.valueOf(LocalDate.now()));
                    stmt.setString(5, "123456789");
                    stmt.setString(6, "employee" + i + "@example.com");
                    stmt.executeUpdate();
                    if (i % 100 == 0) {
                        conn.commit();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return System.currentTimeMillis() - startTime;
    }

    private long testSingleTransactionBatch() {
        long startTime = System.currentTimeMillis();
        try (Connection conn = JdbcUtils.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO Employees (first_name, last_name, role, hire_date, phone, email) VALUES (?, ?, ?,?,?,?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORDS_COUNT; i++) {
                    stmt.setString(1, "FirstName_" + i);
                    stmt.setString(2, "LastName_" + i);
                    stmt.setString(3, "Sales");
                    stmt.setDate(4, Date.valueOf(LocalDate.now()));
                    stmt.setString(5, "123456789");
                    stmt.setString(6, "employee" + i + "@example.com");
                    stmt.addBatch();

                    if (i % 50 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return System.currentTimeMillis() - startTime;
    }


    private void saveToCsvAndPlot(List<Long> autoCommitTimes, List<Long> batchCommitTimes, List<Long> executeBatchTimes, Integer runs) {
        try (FileWriter writer = new FileWriter("performance_results.csv")) {
            writer.append("Run,Auto-Commit(ms),Commit-in-Lots(ms),ExecuteBatch(ms)\n");
            for (int i = 0; i < runs; i++) {
                writer.append(String.valueOf(i + 1)).append(",")
                        .append(String.valueOf(autoCommitTimes.get(i))).append(",")
                        .append(String.valueOf(batchCommitTimes.get(i))).append(",")
                        .append(String.valueOf(executeBatchTimes.get(i))).append("\n");
            }
            System.out.println("\nRezultatele au fost salvate in 'performance_results.csv'");
        } catch (IOException e) {
            System.err.println("Eroare la salvarea CSV-ului: " + e.getMessage());
        }

        long avgAuto = autoCommitTimes.stream().reduce(0L,Long::sum) / runs;
        long avgBatch = batchCommitTimes.stream().reduce(0L, Long::sum) / runs;
        long avgExec = executeBatchTimes.stream().reduce(0L, Long::sum) / runs;

        CategoryChart barChart = new CategoryChartBuilder()
                .width(800).height(600)
                .title("Comparatie Performanta Insert (5000 Randuri)")
                .xAxisTitle("Abordare")
                .yAxisTitle("Timp (Milisecunde) - Mai putin e mai bine")
                .build();

        barChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        barChart.addSeries("Timp Mediu (ms)",
                Arrays.asList("1. Auto-commit", "2. Batch (100)", "3. ExecuteBatch (50)"),
                Arrays.asList(avgAuto, avgBatch, avgExec));

        List<Double> xData = new ArrayList<>();
        List<Double> autoData = new ArrayList<>();
        List<Double> batchData = new ArrayList<>();
        List<Double> execData = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            xData.add((double) (i + 1));
            autoData.add(autoCommitTimes.get(i).doubleValue());
            batchData.add(batchCommitTimes.get(i).doubleValue());
            execData.add(executeBatchTimes.get(i).doubleValue());
        }

        XYChart lineChart = new XYChartBuilder()
                .width(800).height(600)
                .title("Evolutia Timpilor pe Rulari")
                .xAxisTitle("Rulare")
                .yAxisTitle("Timp (ms)")
                .build();

        lineChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        lineChart.addSeries("1. Auto-commit", xData, autoData);
        lineChart.addSeries("2. Batch (100)", xData, batchData);
        lineChart.addSeries("3. ExecuteBatch (50)", xData, execData);

        new SwingWrapper<>(barChart).displayChart();
        new SwingWrapper<>(lineChart).displayChart();
    }
}