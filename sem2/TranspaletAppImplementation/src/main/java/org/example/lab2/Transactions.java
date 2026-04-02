package org.example.lab2;

import org.example.repository.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiConsumer;

class Transactions {

    private void resetData() {
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmtDelete = conn.prepareStatement("DELETE FROM Employees WHERE email IN ('emp1@test.com', 'emp2@test.com', 'emp3@test.com')");
             PreparedStatement stmtInsert = conn.prepareStatement(
                     "INSERT INTO Employees (first_name, last_name, role, hire_date, phone, email) VALUES " +
                             "('Ion', 'Popescu', 'Technician', '2024-01-01', '0700000001', 'emp1@test.com'), " +
                             "('Maria', 'Ionescu', 'Sales', '2024-01-01', '0700000002', 'emp2@test.com')")) {

            stmtDelete.executeUpdate();
            stmtInsert.executeUpdate();
            System.out.println("--- Datele au fost resetate la starea initiala ---");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDatabaseState() {
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Employees");
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n--- Starea curenta a bazei de date ---");
            while (rs.next()) {
                System.out.printf("ID: %s | Name: %s %s | Role: %s | Email: %s%n",
                        rs.getString("employee_id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("role"), rs.getString("email"));
            }
            System.out.println("---------------------------------------\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void demoDirtyRead() throws InterruptedException {
        resetData();
        System.out.println("\n=== A. Dirty Read ===");
        showDatabaseState();

        Thread threadA = new Thread(() -> {
            try (Connection connA = JdbcUtils.getConnection()) {
                connA.setAutoCommit(false);
                System.out.println("Tranzactia A: BEGIN TRANSACTION");

                try (PreparedStatement stmt = connA.prepareStatement("UPDATE Employees SET role = 'Manager' WHERE email = 'emp1@test.com'")) {
                    stmt.executeUpdate();
                    System.out.println("Tranzactia A: Rol actualizat la 'Manager' (necomis)");

                    System.out.println("Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa citeasca valoarea necomisa...");
                    Thread.sleep(3000);

                    connA.rollback();
                    System.out.println("Tranzactia A: ROLLBACK efectuat!");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread threadB = new Thread(() -> {
            try {
                System.out.println("Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut update-ul necomis...");
                Thread.sleep(1000);
                try (Connection connB = JdbcUtils.getConnection()) {
                    connB.setAutoCommit(false);
                    connB.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    System.out.println("Tranzactia B: BEGIN TRANSACTION (READ UNCOMMITTED)");

                    try (PreparedStatement stmt = connB.prepareStatement("SELECT role FROM Employees WHERE email = 'emp1@test.com'");
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("Tranzactia B: Valoarea rolului citita este: '" + rs.getString("role") + "'");
                        }
                    }
                    connB.commit();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        threadA.start(); threadB.start();
        threadA.join(); threadB.join();
        showDatabaseState();
    }

    public void demoNonRepeatableRead() throws InterruptedException {
        resetData();
        System.out.println("\n=== B. Non-Repeatable Read ===");
        showDatabaseState();

        Thread threadA = new Thread(() -> {
            try (Connection connA = JdbcUtils.getConnection()) {
                connA.setAutoCommit(false);
                connA.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                System.out.println("Tranzactia A: BEGIN TRANSACTION (READ COMMITTED)");

                try (PreparedStatement stmt = connA.prepareStatement("SELECT role FROM Employees WHERE email = 'emp1@test.com'")) {
                    ResultSet rs1 = stmt.executeQuery();
                    if (rs1.next()) System.out.println("Tranzactia A - Prima citire: '" + rs1.getString("role") + "'");

                    System.out.println("Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa faca update-ul si commit-ul...");
                    Thread.sleep(3000);

                    ResultSet rs2 = stmt.executeQuery();
                    if (rs2.next()) System.out.println("Tranzactia A - A doua citire: '" + rs2.getString("role") + "'");
                }
                connA.commit();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread threadB = new Thread(() -> {
            try {
                System.out.println("Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut prima citire...");
                Thread.sleep(1000);
                try (Connection connB = JdbcUtils.getConnection()) {
                    connB.setAutoCommit(false);
                    System.out.println("Tranzactia B: BEGIN TRANSACTION");

                    try (PreparedStatement stmt = connB.prepareStatement("UPDATE Employees SET role = 'Manager' WHERE email = 'emp1@test.com'")) {
                        stmt.executeUpdate();
                        connB.commit();
                        System.out.println("Tranzactia B: Rol actualizat la 'Manager' si comis.");
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        threadA.start(); threadB.start();
        threadA.join(); threadB.join();
        showDatabaseState();

    }

    public void demoPhantomRead() throws InterruptedException {
        resetData();
        System.out.println("\n=== C. Phantom Read ===");
        showDatabaseState();

        Thread threadA = new Thread(() -> {
            try (Connection connA = JdbcUtils.getConnection()) {
                connA.setAutoCommit(false);
                connA.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                System.out.println("Tranzactia A: BEGIN TRANSACTION (REPEATABLE READ)");

                try (PreparedStatement stmt = connA.prepareStatement("SELECT COUNT(*) as nr FROM Employees WHERE role = 'Sales'")) {
                    ResultSet rs1 = stmt.executeQuery();
                    if (rs1.next()) System.out.println("Tranzactia A - Prima numaratoare (Sales): " + rs1.getInt("nr"));

                    System.out.println("Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa insereze un nou angajat cu rol 'Sales' si sa comita...");
                    Thread.sleep(3000);

                    ResultSet rs2 = stmt.executeQuery();
                    if (rs2.next()) System.out.println("Tranzactia A - A doua numaratoare (Sales): " + rs2.getInt("nr"));
                }
                connA.commit();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread threadB = new Thread(() -> {
            try {
                System.out.println("Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut prima numaratoare...");
                Thread.sleep(1000);
                try (Connection connB = JdbcUtils.getConnection()) {
                    connB.setAutoCommit(false);
                    System.out.println("Tranzactia B: BEGIN TRANSACTION");

                    String insertSql = "INSERT INTO Employees (first_name, last_name, role, hire_date, phone, email) " +
                            "VALUES ('Angajat Nou', 'Test', 'Sales', '2024-02-01', '0700000003', 'emp3@test.com')";
                    try (PreparedStatement stmt = connB.prepareStatement(insertSql)) {
                        stmt.executeUpdate();
                        connB.commit();
                        System.out.println("Tranzactia B: Angajat nou inserat cu rolul 'Sales' si comis.");
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        threadA.start(); threadB.start();
        threadA.join(); threadB.join();
        showDatabaseState();

    }

    public void demoLostUpdate() throws InterruptedException {
        resetData();
        System.out.println("\n=== D. Lost Update ===");
        showDatabaseState();

        BiConsumer<String, String> task = (threadName, suffix) -> {
            try (Connection conn = JdbcUtils.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println("Tranzactia " + threadName + ": BEGIN TRANSACTION");

                String currentName = "";
                try (PreparedStatement stmt = conn.prepareStatement("SELECT first_name FROM Employees WHERE email = 'emp1@test.com'")) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) currentName = rs.getString("first_name");
                    System.out.println("Tranzactia " + threadName + ": A citit prenumele '" + currentName + "'");
                }

                String newName = currentName + suffix;
                System.out.println("Tranzactia " + threadName + ": A calculat noul prenume '" + newName + "'");

                Thread.sleep(2000);

                try (PreparedStatement stmt2 = conn.prepareStatement("UPDATE Employees SET first_name = ? WHERE email = 'emp1@test.com'")) {
                    stmt2.setString(1, newName);
                    stmt2.executeUpdate();
                }
                conn.commit();
                System.out.println("Tranzactia " + threadName + ": COMMIT cu succes (Prenume setat la '" + newName + "')");

            } catch (Exception e) { e.printStackTrace(); }
        };

        Thread threadA = new Thread(() -> task.accept("A", "-A"));
        Thread threadB = new Thread(() -> task.accept("B", "-B"));

        threadA.start(); threadB.start();
        threadA.join(); threadB.join();
        showDatabaseState();

    }

    public void demoDeadlock() throws InterruptedException {
        resetData();
        System.out.println("\n=== 2. Demonstratie Deadlock ===");
        showDatabaseState();

        Thread threadA = new Thread(() -> {
            try (Connection connA = JdbcUtils.getConnection()) {
                connA.setAutoCommit(false);
                System.out.println("Tranzactia A: BEGIN TRANSACTION");

                try (PreparedStatement stmt1 = connA.prepareStatement("UPDATE Employees SET role = 'Admin' WHERE email = 'emp1@test.com'")) {
                    stmt1.executeUpdate();
                    System.out.println("Tranzactia A: A blocat Angajat 1 (emp1)");
                }

                Thread.sleep(2000);

                System.out.println("Tranzactia A: Incearca sa blocheze Angajat 2 (emp2)...");
                try (PreparedStatement stmt2 = connA.prepareStatement("UPDATE Employees SET role = 'Admin' WHERE email = 'emp2@test.com'")) {
                    stmt2.executeUpdate();
                }
                connA.commit();
            } catch (SQLException e) {
                System.err.println("Tranzactia A ERROR (Deadlock): " + e.getMessage());
            } catch (InterruptedException e) { e.printStackTrace(); }
        });

        Thread threadB = new Thread(() -> {
            try (Connection connB = JdbcUtils.getConnection()) {
                connB.setAutoCommit(false);
                System.out.println("Tranzactia B: BEGIN TRANSACTION");

                try (PreparedStatement stmt1 = connB.prepareStatement("UPDATE Employees SET role = 'Manager' WHERE email = 'emp2@test.com'")) {
                    stmt1.executeUpdate();
                    System.out.println("Tranzactia B: A blocat Angajat 2 (emp2)");
                }

                Thread.sleep(2000);

                System.out.println("Tranzactia B: Incearca sa blocheze Angajat 1 (emp1)...");
                try (PreparedStatement stmt2 = connB.prepareStatement("UPDATE Employees SET role = 'Manager' WHERE email = 'emp1@test.com'")) {
                    stmt2.executeUpdate();
                }
                connB.commit();
            } catch (SQLException e) {
                System.err.println("Tranzactia B ERROR (Deadlock): " + e.getMessage());
            } catch (InterruptedException e) { e.printStackTrace(); }
        });

        threadA.start(); threadB.start();
        threadA.join(); threadB.join();
        showDatabaseState();

    }

}