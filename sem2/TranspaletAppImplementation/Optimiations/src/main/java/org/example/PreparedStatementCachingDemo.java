package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class PreparedStatementCachingDemo {

    private static final int    QUERIES = 1000;
    private static final int    ITERS   = 5;
    private static final String HQL     = "SELECT e FROM Employee e WHERE e.id = :id";

    public static void run() {
        SessionFactory sf = HibernateUtil.getSessionFactory();

        System.out.println("  PREPARED STATEMENT CACHING DEMO");
        System.out.printf("  Queries per iteration: %d  |  Iterations: %d%n%n", QUERIES, ITERS);

        System.out.println("  Task A - fara reutilizarea statement-urilor");
        double timeA = taskA(sf);

        System.out.println("\n  Task B - cu reutilizarea statement-urilor");
        double timeB = taskB(sf);

        printSummary(timeA, timeB);
    }

    // Task A: new Query object for every call (fara reutilizare)
    private static double taskA(SessionFactory sf) {
        long total = 0;
        for (int iter = 0; iter < ITERS; iter++) {
            long t0 = System.nanoTime();
            try (Session s = sf.openSession()) {
                for (int i = 0; i < QUERIES; i++) {
                    s.createQuery(HQL, Employee.class)
                     .setParameter("id", (long) i)
                     .getSingleResultOrNull();
                }
            }
            long elapsed = System.nanoTime() - t0;
            total += elapsed;
            System.out.printf("  iter %d : %,d ms%n", iter + 1, elapsed / 1_000_000);
        }
        double avg = total / (double) ITERS / 1_000_000.0;
        System.out.printf("  Medie  : %.1f ms%n", avg);
        return avg;
    }

    private static double taskB(SessionFactory sf) {
        long total = 0;
        for (int iter = 0; iter < ITERS; iter++) {
            long t0 = System.nanoTime();
            try (Session s = sf.openSession()) {
                var query = s.createQuery(HQL, Employee.class);
                for (int i = 0; i < QUERIES; i++) {
                    query.setParameter("id", (long) i);
                    query.getSingleResultOrNull();
                }
            }
            long elapsed = System.nanoTime() - t0;
            total += elapsed;
            System.out.printf("  iter %d : %,d ms%n", iter + 1, elapsed / 1_000_000);
        }
        double avg = total / (double) ITERS / 1_000_000.0;
        System.out.printf("  Medie  : %.1f ms%n", avg);
        return avg;
    }

    private static void printSummary(double a, double b) {
        System.out.printf("  COMPARATIE  (%d query-uri x %d iteratii)%n", QUERIES, ITERS);
        System.out.printf("%-48s  %10s%n", "Varianta", "Medie (ms)");
        System.out.printf("%-48s  %7.1f ms%n", "A - fara reutilizare  (createQuery() per call)", a);
        System.out.printf("%-48s  %7.1f ms%n", "B - cu reutilizare    (Query object refolosit)",  b);
    }
}