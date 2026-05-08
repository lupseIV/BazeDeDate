package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HealthCheck {

    private static final long   RESPONSE_THRESHOLD_MS = 200;
    private static final long   MIN_EMPLOYEES         = 1_000;
    private static final long   MIN_DEPARTMENTS       = 1;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void run() {
        SessionFactory sf = HibernateUtil.getSessionFactory();

        System.out.println("\n" + "=".repeat(65));
        System.out.printf("  DB HEALTH CHECK  [%s]%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("=".repeat(65));

        List<String[]> results = new ArrayList<>();
        results.add(checkSessionFactory(sf));
        results.add(checkConnectivity(sf));
        results.add(checkTable(sf, "departments", MIN_DEPARTMENTS));
        results.add(checkTable(sf, "employees",   MIN_EMPLOYEES));
        results.add(checkL2Cache(sf));
        results.add(checkQueryCache(sf));
        results.add(checkStatisticsEnabled(sf));
        results.add(checkResponseTime(sf));

        long passed = results.stream().filter(r -> "PASS".equals(r[0])).count();
        long warned = results.stream().filter(r -> "WARN".equals(r[0])).count();
        long failed = results.stream().filter(r -> "FAIL".equals(r[0])).count();

        for (String[] r : results) {
            String badge = switch (r[0]) {
                case "PASS" -> "[PASS]";
                case "WARN" -> "[WARN]";
                default     -> "[FAIL]";
            };
            System.out.printf("  %s  %s%n", badge, r[1]);
        }

        String overall = failed > 0 ? "DOWN" : warned > 0 ? "DEGRADED" : "HEALTHY";
        System.out.printf("  Overall: %-10s  passed=%d  warned=%d  failed=%d%n%n",
                overall, passed, warned, failed);
    }

    // ── Individual checks ──────────────────────────────────────────────────────

    private static String[] checkSessionFactory(SessionFactory sf) {
        try {
            return sf.isClosed()
                    ? fail("SessionFactory is closed")
                    : pass("SessionFactory: initialized and open");
        } catch (Exception e) {
            return fail("SessionFactory error: " + e.getMessage());
        }
    }

    private static String[] checkConnectivity(SessionFactory sf) {
        try {
            long t0 = System.currentTimeMillis();
            try (Session s = sf.openSession()) {
                s.createNativeQuery("SELECT 1", Integer.class).getSingleResult();
            }
            return pass(String.format("DB connectivity: OK (%d ms)", System.currentTimeMillis() - t0));
        } catch (Exception e) {
            return fail("DB connection failed: " + e.getMessage());
        }
    }

    private static String[] checkTable(SessionFactory sf, String table, long minRows) {
        try {
            long count;
            try (Session s = sf.openSession()) {
                count = s.createNativeQuery(
                        "SELECT COUNT(*) FROM " + table, Long.class).getSingleResult();
            }
            return count >= minRows
                    ? pass(String.format("Table '%s': %,d rows", table, count))
                    : fail(String.format("Table '%s': %,d rows (need >= %,d)", table, count, minRows));
        } catch (Exception e) {
            return fail("Table '" + table + "' check failed: " + e.getMessage());
        }
    }

    private static String[] checkL2Cache(SessionFactory sf) {
        try {
            sf.getCache().contains(Department.class, -1L); // safe probe (returns false)
            return pass("L2 entity cache: ENABLED (EhCache 3 / JCache)");
        } catch (Exception e) {
            return fail("L2 cache not available: " + e.getMessage());
        }
    }

    private static String[] checkQueryCache(SessionFactory sf) {
        try {
            boolean enabled = sf.getSessionFactoryOptions().isQueryCacheEnabled();
            return enabled
                    ? pass("Query result cache: ENABLED")
                    : warn("Query result cache: DISABLED (set hibernate.cache.use_query_cache=true)");
        } catch (Exception e) {
            return warn("Query cache status unknown: " + e.getMessage());
        }
    }

    private static String[] checkStatisticsEnabled(SessionFactory sf) {
        return sf.getStatistics().isStatisticsEnabled()
                ? pass("Hibernate statistics: ENABLED")
                : warn("Hibernate statistics: DISABLED (set hibernate.generate_statistics=true)");
    }

    private static String[] checkResponseTime(SessionFactory sf) {
        try {
            long t0 = System.currentTimeMillis();
            try (Session s = sf.openSession()) {
                s.createQuery("SELECT COUNT(e) FROM Employee e", Long.class).getSingleResult();
            }
            long ms = System.currentTimeMillis() - t0;
            return ms < RESPONSE_THRESHOLD_MS
                    ? pass(String.format("COUNT(*) response: %d ms (< %d ms)", ms, RESPONSE_THRESHOLD_MS))
                    : warn(String.format("COUNT(*) response: %d ms (> %d ms threshold)", ms, RESPONSE_THRESHOLD_MS));
        } catch (Exception e) {
            return fail("Response-time check failed: " + e.getMessage());
        }
    }

    // ── Result builders ───────────────────────────────────────────────────────

    private static String[] pass(String msg) { return new String[]{"PASS", msg}; }
    private static String[] warn(String msg) { return new String[]{"WARN", msg}; }
    private static String[] fail(String msg) { return new String[]{"FAIL", msg}; }
}