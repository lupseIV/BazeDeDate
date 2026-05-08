package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class BulkUpdateBenchmark {

    private static final int    BATCH_SIZE = 50;
    private static final int    ITERS      = 3;
    private static final double RAISE      = 1.1;

    public static void run() {
        SessionFactory sf       = HibernateUtil.getSessionFactory();
        long           rowCount = countEmployees(sf);

        System.out.println("  BULK UPDATE BENCHMARK  -  Salary raise 10%");
        System.out.printf("  Rows: %,d  |  Batch size: %d  |  Iterations: %d%n%n",
                rowCount, BATCH_SIZE, ITERS);

        System.out.println("  Approach 1 - Individual updates  (dirty checking, N UPDATEs)");
        double timeA = bench(sf, BulkUpdateBenchmark::approach1);

        System.out.println("\n  Approach 2 - Bulk JPQL UPDATE  (single statement, no entity load)");
        double timeB = bench(sf, BulkUpdateBenchmark::approach2);

        System.out.printf("%n  Approach 3 - Batch flush/clear every %d rows (bounded memory)%n", BATCH_SIZE);
        double timeC = bench(sf, BulkUpdateBenchmark::approach3);

        printSummary(timeA, timeB, timeC, rowCount);
    }

    // ── Approach 1: load all → modify → commit (Hibernate dirty checking) ──────

    private static void approach1(SessionFactory sf) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            List<Employee> emps = s
                    .createQuery("SELECT e FROM Employee e", Employee.class)
                    .getResultList();
            for (Employee emp : emps) {
                emp.setSalary(emp.getSalary() * RAISE);
            }
            s.getTransaction().commit();
        }
    }

    // ── Approach 2: single JPQL bulk UPDATE ────────────────────────────────────

    private static void approach2(SessionFactory sf) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            s.createMutationQuery("UPDATE Employee e SET e.salary = e.salary * :r")
                    .setParameter("r", RAISE)
                    .executeUpdate();
            s.getTransaction().commit();
        }
    }

    // ── Approach 3: paginated load + flush/clear every BATCH_SIZE rows ─────────

    private static void approach3(SessionFactory sf) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();

            List<Employee> emps = s
                    .createQuery("SELECT e FROM Employee e", Employee.class)
                    .getResultList();
            for(int i = 0; i < emps.size(); i++) {
                Employee emp = emps.get(i);
                emp.setSalary(emp.getSalary() * RAISE);

                if ( i % BATCH_SIZE == 0 && i > 0){
                    s.flush();
                    s.clear();
                }
            }
            s.getTransaction().commit();

        }
    }

    // ── Timing harness ─────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Approach { void run(SessionFactory sf); }

    private static double bench(SessionFactory sf, Approach approach) {
        approach.run(sf);
        resetSalaries(sf);

        long total = 0;
        for (int i = 0; i < ITERS; i++) {
            long t0 = System.nanoTime();
            approach.run(sf);
            long elapsed = System.nanoTime() - t0;
            total += elapsed;

            resetSalaries(sf);
            System.out.printf("  iter %d : %,d ms%n", i + 1, elapsed / 1_000_000);
        }

        double avg = total / (double) ITERS / 1_000_000.0;
        System.out.printf("  Average : %.1f ms%n", avg);
        return avg;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void resetSalaries(SessionFactory sf) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            s.createMutationQuery("UPDATE Employee e SET e.salary = e.salary / :r")
                    .setParameter("r", RAISE)
                    .executeUpdate();
            s.getTransaction().commit();
        }
    }

    private static long countEmployees(SessionFactory sf) {
        try (Session s = sf.openSession()) {
            return s.createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                    .getSingleResult();
        }
    }

    // ── Summary ────────────────────────────────────────────────────────────────

    private static void printSummary(double a, double b, double c, long rows) {
        System.out.printf("  RESULTS  (%d iterations, %,d rows, +%.0f%% raise)%n",
                ITERS, rows, (RAISE - 1) * 100);
        System.out.printf("%-44s  %12s  %10s%n", "Approach", "Avg (ms)", "vs Bulk");
        System.out.printf("%-44s  %9.1f ms  %9s%n",  "1 - Individual (dirty checking)", a, "baseline");
        System.out.printf("%-44s  %9.1f ms  %8.1fx%n", "2 - Bulk JPQL UPDATE",           b, a / b);
        System.out.printf("%-44s  %9.1f ms  %8.1fx%n", "3 - Batch flush/clear/" + BATCH_SIZE, c, a / c);
    }
}