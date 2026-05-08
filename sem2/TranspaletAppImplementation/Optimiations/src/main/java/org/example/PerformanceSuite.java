package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated performance benchmark suite.
 * Runs all optimisation categories, prints a consolidated summary table,
 * and writes a structured report to performance_report.txt.
 * Bonus: +7 performance testing suite with automated benchmarks.
 */
public class PerformanceSuite {

    private static final int    ITERS      = 3;
    private static final int    PAGE_SIZE  = 25;
    private static final String REPORT     = "performance_report.txt";
    private static final double RAISE      = 1.1;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void run() {
        SessionFactory sf    = HibernateUtil.getSessionFactory();
        Statistics     stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("  AUTOMATED PERFORMANCE SUITE  (all categories, " + ITERS + " iterations each)");
        System.out.println("=".repeat(70));

        // Run health check first
        HealthCheck.run();

        Map<String, double[]> results = new LinkedHashMap<>();

        System.out.println("\n[1/6] N+1 vs JOIN FETCH ...");
        results.put("nplusone", measureNPlusOne(sf));

        System.out.println("[2/6] Indexing ...");
        results.put("indexing", measureIndexing(sf));

        System.out.println("[3/6] Pagination (Offset vs Keyset) ...");
        results.put("pagination", measurePagination(sf));

        System.out.println("[4/6] L2 Cache (miss vs hit) ...");
        results.put("caching", measureCaching(sf));

        System.out.println("[5/6] Bulk Update (individual / bulk / batch) ...");
        results.put("bulk", measureBulkUpdate(sf));

        System.out.println("[6/6] Prepared Statement Caching ...");
        results.put("ps", measurePSCaching(sf));

        printConsolidatedSummary(results);
        writeReport(results, sf);

        System.out.println("\n  Full report saved to: " + REPORT);
    }

    // ── Benchmark: N+1 vs JOIN FETCH ─────────────────────────────────────────

    private static double[] measureNPlusOne(SessionFactory sf) {
        double n1ms = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                List<Department> depts = s.createQuery(
                        "SELECT d FROM Department d", Department.class).getResultList();
                for (Department d : depts) d.getEmployees().size();
            }
        });
        double jfms = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                List<Department> depts = s.createQuery(
                        "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees",
                        Department.class).getResultList();
                for (Department d : depts) d.getEmployees().size();
            }
        });
        System.out.printf("    N+1: %.1f ms   JOIN FETCH: %.1f ms   speedup: %.1fx%n",
                n1ms, jfms, n1ms / jfms);
        return new double[]{n1ms, jfms};
    }

    // ── Benchmark: indexing ───────────────────────────────────────────────────

    private static double[] measureIndexing(SessionFactory sf) {
        // Drop index, measure, create, measure again
        executeDDL(sf, "DROP INDEX IF EXISTS idx_suite_email");
        double noIdx = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                s.createNativeQuery(
                        "SELECT * FROM employees WHERE email = 'user_500@example.com'", Employee.class)
                 .getResultList();
            }
        });

        executeDDL(sf, "CREATE INDEX IF NOT EXISTS idx_suite_email ON employees(email)");
        double withIdx = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                s.createNativeQuery(
                        "SELECT * FROM employees WHERE email = 'user_500@example.com'", Employee.class)
                 .getResultList();
            }
        });

        System.out.printf("    Without index: %.3f ms   With index: %.3f ms   speedup: %.1fx%n",
                noIdx, withIdx, noIdx / withIdx);
        return new double[]{noIdx, withIdx};
    }

    // ── Benchmark: pagination ─────────────────────────────────────────────────

    private static double[] measurePagination(SessionFactory sf) {
        PaginationService svc  = new PaginationService(sf);
        long total = svc.countAll();
        int  pages = (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE);
        int  mid   = pages / 2;
        Long midCur = svc.idAtOffset(mid * PAGE_SIZE - 1);

        double oFirst = avg(ITERS, () -> svc.getPage(0, PAGE_SIZE));
        double oMid   = avg(ITERS, () -> svc.getPage(mid, PAGE_SIZE));
        double kFirst = avg(ITERS, () -> svc.getPageAfter(0L, PAGE_SIZE));
        double kMid   = avg(ITERS, () -> svc.getPageAfter(midCur, PAGE_SIZE));

        System.out.printf("    Offset  first=%.1f ms  mid=%.1f ms%n", oFirst, oMid);
        System.out.printf("    Keyset  first=%.1f ms  mid=%.1f ms%n", kFirst, kMid);
        return new double[]{oFirst, oMid, kFirst, kMid};
    }

    // ── Benchmark: L2 cache ───────────────────────────────────────────────────

    private static double[] measureCaching(SessionFactory sf) {
        sf.getCache().evictEntityData(Department.class);

        double miss = avg(ITERS, () -> {
            sf.getCache().evictEntityData(Department.class);
            try (Session s = sf.openSession()) {
                for (long id = 1; id <= 20; id++) s.get(Department.class, id);
            }
        });
        double hit = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                for (long id = 1; id <= 20; id++) s.get(Department.class, id);
            }
        });

        System.out.printf("    20 depts  MISS: %.1f ms   HIT: %.1f ms   speedup: %.1fx%n",
                miss, hit, miss / hit);
        return new double[]{miss, hit};
    }

    // ── Benchmark: bulk update ────────────────────────────────────────────────

    private static double[] measureBulkUpdate(SessionFactory sf) {
        Runnable resetFn = () -> {
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                s.createMutationQuery("UPDATE Employee e SET e.salary = e.salary / :r")
                 .setParameter("r", RAISE).executeUpdate();
                s.getTransaction().commit();
            }
        };

        double individual = avgWithReset(ITERS, resetFn, () -> {
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                s.createQuery("SELECT e FROM Employee e", Employee.class)
                 .getResultList()
                 .forEach(e -> e.setSalary(e.getSalary() * RAISE));
                s.getTransaction().commit();
            }
        });

        double bulk = avgWithReset(ITERS, resetFn, () -> {
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                s.createMutationQuery("UPDATE Employee e SET e.salary = e.salary * :r")
                 .setParameter("r", RAISE).executeUpdate();
                s.getTransaction().commit();
            }
        });

        double batch = avgWithReset(ITERS, resetFn, () -> {
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                int offset = 0;
                List<Employee> b;
                do {
                    b = s.createQuery("SELECT e FROM Employee e ORDER BY e.id", Employee.class)
                         .setFirstResult(offset).setMaxResults(50).getResultList();
                    b.forEach(e -> e.setSalary(e.getSalary() * RAISE));
                    s.flush(); s.clear();
                    offset += b.size();
                } while (b.size() == 50);
                s.getTransaction().commit();
            }
        });

        System.out.printf("    Individual: %.0f ms   Bulk JPQL: %.0f ms   Batch-50: %.0f ms%n",
                individual, bulk, batch);
        return new double[]{individual, bulk, batch};
    }

    // ── Benchmark: PS caching ─────────────────────────────────────────────────

    private static double[] measurePSCaching(SessionFactory sf) {
        final String hql = "SELECT e FROM Employee e WHERE e.id = :id";
        // warm plan cache
        try (Session s = sf.openSession()) {
            s.createQuery(hql, Employee.class).setParameter("id", 1L).getResultList();
        }

        double newQ = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                for (int i = 1; i <= 200; i++)
                    s.createQuery(hql, Employee.class).setParameter("id", (long) i).getResultList();
            }
        });
        double reuse = avg(ITERS, () -> {
            try (Session s = sf.openSession()) {
                var q = s.createQuery(hql, Employee.class);
                for (int i = 1; i <= 200; i++) { q.setParameter("id", (long) i); q.getResultList(); }
            }
        });

        System.out.printf("    200 queries  new createQuery: %.1f ms   reused Query: %.1f ms%n",
                newQ, reuse);
        return new double[]{newQ, reuse};
    }

    // ── Consolidated summary table ────────────────────────────────────────────

    private static void printConsolidatedSummary(Map<String, double[]> r) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  PERFORMANCE SUITE – CONSOLIDATED RESULTS");
        System.out.println("=".repeat(70));

        double[] np  = r.get("nplusone");
        double[] ix  = r.get("indexing");
        double[] pg  = r.get("pagination");
        double[] ca  = r.get("caching");
        double[] bu  = r.get("bulk");
        double[] ps  = r.get("ps");

        System.out.printf("  %-42s  %10s  %10s  %8s%n", "Category / Metric", "Before", "After", "Speedup");
        System.out.println("  " + "-".repeat(66));
        row2("N+1  vs  JOIN FETCH",              np[0],  np[1]);
        row2("Index  email search (no/yes idx)", ix[0],  ix[1]);
        row2("Offset  first / mid page",         pg[0],  pg[1]);
        row2("Keyset  first / mid page",         pg[2],  pg[3]);
        row2("Cache  20 depts MISS / HIT",       ca[0],  ca[1]);
        row2("Bulk  individual / JPQL UPDATE",   bu[0],  bu[1]);
        row2("PS  new query / reused query",      ps[0],  ps[1]);
        System.out.println("=".repeat(70));
    }

    private static void row2(String label, double a, double b) {
        System.out.printf("  %-42s  %7.1f ms  %7.1f ms  %6.1fx%n",
                label, a, b, b > 0 ? a / b : 0);
    }

    // ── Report file ───────────────────────────────────────────────────────────

    private static void writeReport(Map<String, double[]> r, SessionFactory sf) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(REPORT))) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pw.println("=".repeat(70));
            pw.println("  DB OPTIMIZATION PERFORMANCE REPORT");
            pw.printf ("  Generated: %s  |  Iterations: %d%n", ts, ITERS);
            pw.println("=".repeat(70));

            double[] np = r.get("nplusone");
            section(pw, "1. N+1 PROBLEM  (NplusOneDemo)");
            pw.printf("   N+1 (lazy load per department)  : %8.1f ms%n", np[0]);
            pw.printf("   JOIN FETCH (single query)        : %8.1f ms%n", np[1]);
            pw.printf("   Speedup                          : %8.1fx%n",  np[0] / np[1]);
            pw.println("   Recommendation: always use JOIN FETCH for collections you will iterate.");

            double[] ix = r.get("indexing");
            section(pw, "2. INDEXING  (IndexingBenchmark)");
            pw.printf("   Email search WITHOUT index       : %8.3f ms%n", ix[0]);
            pw.printf("   Email search WITH B-tree index   : %8.3f ms%n", ix[1]);
            pw.printf("   Speedup                          : %8.1fx%n",  ix[0] / ix[1]);
            pw.println("   Recommendation: index columns used in WHERE / ORDER BY / JOIN ON.");

            double[] pg = r.get("pagination");
            section(pw, "3. PAGINATION  (PaginationsTest)");
            pw.printf("   Offset  first page               : %8.1f ms%n", pg[0]);
            pw.printf("   Offset  mid   page               : %8.1f ms%n", pg[1]);
            pw.printf("   Keyset  first page               : %8.1f ms%n", pg[2]);
            pw.printf("   Keyset  mid   page               : %8.1f ms%n", pg[3]);
            pw.println("   Recommendation: keyset for infinite scroll; offset when total count needed.");

            double[] ca = r.get("caching");
            section(pw, "4. L2 CACHING  (CachingDemo)");
            pw.printf("   20 departments – cache MISS      : %8.1f ms%n", ca[0]);
            pw.printf("   20 departments – cache HIT       : %8.1f ms%n", ca[1]);
            pw.printf("   Speedup                          : %8.1fx%n",  ca[0] / ca[1]);
            pw.println("   Recommendation: cache read-heavy parent entities (departments, categories).");
            pw.println("   Use READ_WRITE for entities that are also updated.");

            double[] bu = r.get("bulk");
            section(pw, "5. BULK UPDATE  (BulkUpdateBenchmark)");
            pw.printf("   Individual updates (dirty check) : %8.1f ms%n", bu[0]);
            pw.printf("   Bulk JPQL UPDATE                 : %8.1f ms%n", bu[1]);
            pw.printf("   Batch flush/clear (size=50)      : %8.1f ms%n", bu[2]);
            pw.printf("   Bulk speedup vs individual       : %8.1fx%n",  bu[0] / bu[1]);
            pw.println("   Recommendation: JPQL bulk for simple updates; batch when entity logic runs.");

            double[] ps = r.get("ps");
            section(pw, "6. PREPARED STATEMENT CACHING  (PreparedStatementCachingDemo)");
            pw.printf("   200 queries – new createQuery()  : %8.1f ms%n", ps[0]);
            pw.printf("   200 queries – reused Query obj   : %8.1f ms%n", ps[1]);
            pw.printf("   Speedup                          : %8.1fx%n",  ps[0] / ps[1]);
            pw.println("   Recommendation: reuse Query objects inside a session; use PreparedStatement");
            pw.println("   for raw JDBC to avoid re-parse at DB level and prevent SQL injection.");

            section(pw, "GLOBAL HIBERNATE STATISTICS");
            Statistics stats = sf.getStatistics();
            pw.printf("   L2 cache hits   : %d%n", stats.getSecondLevelCacheHitCount());
            pw.printf("   L2 cache misses : %d%n", stats.getSecondLevelCacheMissCount());
            pw.printf("   Query cache hits: %d%n", stats.getQueryCacheHitCount());
            pw.printf("   Queries executed: %d%n", stats.getQueryExecutionCount());
            pw.printf("   Stmts prepared  : %d%n", stats.getPrepareStatementCount());

            pw.println("\n" + "=".repeat(70));
            pw.println("  END OF REPORT");
            pw.println("=".repeat(70));

        } catch (IOException e) {
            System.err.println("  [WARN] Could not write report file: " + e.getMessage());
        }
    }

    private static void section(PrintWriter pw, String title) {
        pw.println();
        pw.println("  " + "-".repeat(66));
        pw.println("  " + title);
        pw.println("  " + "-".repeat(66));
    }

    // ── Timing helpers ────────────────────────────────────────────────────────

    private static double avg(int iters, Runnable action) {
        long total = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            action.run();
            total += System.nanoTime() - t0;
        }
        return total / (double) iters / 1_000_000.0;
    }

    private static double avgWithReset(int iters, Runnable reset, Runnable action) {
        reset.run();
        long total = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            action.run();
            total += System.nanoTime() - t0;
            reset.run();
        }
        return total / (double) iters / 1_000_000.0;
    }

    private static void executeDDL(SessionFactory sf, String sql) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            s.createNativeQuery(sql).executeUpdate();
            s.getTransaction().commit();
        }
    }
}
