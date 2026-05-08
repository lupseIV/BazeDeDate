package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class PaginationsTest {

    private static final int PAGE_SIZE = 25;
    private static final int ITERS     = 20;

    public static void run() {
        SessionFactory    sf  = HibernateUtil.getSessionFactory();
        PaginationService svc = new PaginationService(sf);

        long totalRows  = svc.countAll();
        int  totalPgs   = (int) ((totalRows + PAGE_SIZE - 1) / PAGE_SIZE);
        int  midPage    = totalPgs / 2;

        System.out.println("  TASK A - Offset Pagination  (LIMIT / OFFSET)");
        System.out.printf("  Rows: %,d  |  Page size: %d  |  Total pages: %d%n%n",
                totalRows, PAGE_SIZE, totalPgs);

        double[] timesA = taskA(svc, midPage, totalPgs);

        Long midCursor  = svc.idAtOffset(midPage * PAGE_SIZE - 1);
        Long lastCursor = svc.idAtOffset((totalPgs - 1) * PAGE_SIZE - 1);

        System.out.println("  TASK B - Keyset Pagination  (Cursor / Seek)");
        System.out.printf("  Mid-page cursor : id > %d%n", midCursor);
        System.out.printf("  Last-page cursor: id > %d%n%n", lastCursor);

        double[] timesB = taskB(svc, midCursor, lastCursor);

        System.out.println("\nEXPLAIN ANALYZE: mid-page offset vs keyset");
        explainBoth(sf, midPage, midCursor);

        printSummary(timesA, timesB, totalRows, totalPgs, midPage);
    }

    // ── Task A: Offset ─────────────────────────────────────────────────────────

    private static double[] taskA(PaginationService svc, int midPage, int totalPgs) {
        double tFirst = measure(() -> svc.getPage(0,           PAGE_SIZE));
        double tMid   = measure(() -> svc.getPage(midPage,     PAGE_SIZE));
        double tLast  = measure(() -> svc.getPage(totalPgs - 1, PAGE_SIZE));

        System.out.printf("  %-40s : %8.3f ms avg%n", "First page  (page 0)",                tFirst);
        System.out.printf("  %-40s : %8.3f ms avg%n", "Mid page    (page " + midPage + ")",  tMid);
        System.out.printf("  %-40s : %8.3f ms avg%n", "Last page   (page " + (totalPgs-1) + ")", tLast);

        return new double[]{tFirst, tMid, tLast};
    }

    // ── Task B: Keyset ─────────────────────────────────────────────────────────

    private static double[] taskB(PaginationService svc, Long midCursor, Long lastCursor) {
        double tFirst = measure(() -> svc.getPageAfter(0L,         PAGE_SIZE));
        double tMid   = measure(() -> svc.getPageAfter(midCursor,  PAGE_SIZE));
        double tLast  = measure(() -> svc.getPageAfter(lastCursor, PAGE_SIZE));

        System.out.printf("  %-40s : %8.3f ms avg%n", "First page  (lastId = 0)",            tFirst);
        System.out.printf("  %-40s : %8.3f ms avg%n", "Mid page    (lastId = " + midCursor + ")", tMid);
        System.out.printf("  %-40s : %8.3f ms avg%n", "Last page   (lastId = " + lastCursor + ")", tLast);

        return new double[]{tFirst, tMid, tLast};
    }


    private static double measure(Runnable action) {
        long total = 0;
        for (int i = 0; i < ITERS; i++) {
            long t0 = System.nanoTime();
            action.run();
            total += System.nanoTime() - t0;
        }
        return total / (double) ITERS / 1_000_000.0;
    }


    private static void explainBoth(SessionFactory sf, int midPage, Long midCursor) {
        String offsetSql = String.format(
                "EXPLAIN ANALYZE SELECT * FROM employees ORDER BY id LIMIT %d OFFSET %d",
                PAGE_SIZE, (long) midPage * PAGE_SIZE);
        String keysetSql = String.format(
                "EXPLAIN ANALYZE SELECT * FROM employees WHERE id > %d ORDER BY id LIMIT %d",
                midCursor, PAGE_SIZE);

        try (Session session = sf.openSession()) {
            System.out.println("\n>> Strategy A - Offset (shows full sort + skip):");
            List<String> planA = session.createNativeQuery(offsetSql, String.class).getResultList();
            planA.stream().limit(8).forEach(l -> System.out.println("   " + l));

            System.out.println("\n>> Strategy B - Keyset (shows index seek):");
            List<String> planB = session.createNativeQuery(keysetSql, String.class).getResultList();
            planB.stream().limit(8).forEach(l -> System.out.println("   " + l));
        }
    }


    private static void printSummary(double[] a, double[] b,
                                     long totalRows, int totalPgs, int midPage) {
        String[] scenarios = {
            "First page",
            "Mid page  (page " + midPage + ")",
            "Last page (page " + (totalPgs - 1) + ")"
        };

        System.out.printf("  PAGINATION COMPARISON  (%d iterations, page size %d, %,d rows)%n",
                ITERS, PAGE_SIZE, totalRows);
        System.out.printf("%-32s  %13s  %13s  %10s%n",
                "Scenario", "Offset A (ms)", "Keyset B (ms)", "Factor A/B");

        for (int i = 0; i < 3; i++) {
            double factor = (b[i] > 0) ? a[i] / b[i] : 0;
            System.out.printf("%-32s  %10.3f ms  %10.3f ms  %8.1fx%n",
                    scenarios[i], a[i], b[i], factor);
        }

    }
}
