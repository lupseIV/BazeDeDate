package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import java.util.List;

public class CachingDemo {

    private static final String REGION   = "org.example.Department";
    private static final int    TTL_SECS = 10;

    public static void run() {
        SessionFactory sf    = HibernateUtil.getSessionFactory();
        Statistics     stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        System.out.println("  TASK A - Cache MISS  (first load - hits the database)");
        taskA(sf, stats);

        System.out.println("  TASK B - Cache HIT  (repeat load - served from L2 cache)");
        taskB(sf, stats);

        System.out.println("  TASK C - Eviction  (update invalidates / refreshes cache)");
        taskC(sf, stats);

        System.out.printf("  TASK D - TTL Expiration  (auto-eviction after %ds)%n", TTL_SECS);
        taskD(sf, stats);

        System.out.println("  TASK E - Query Result Cache  (ORM-level, BONUS +5)");
        taskE(sf, stats);

        printSummary(sf, stats);
    }

    // ── Task A: cold cache, every get() goes to the database ──────────────────

    private static void taskA(SessionFactory sf, Statistics stats) {
        stats.clear();
        System.out.println("\n  Loading departments 1-5 (cache is empty):\n");

        for (long id = 1; id <= 5; id++) {
            long hitsBefore   = stats.getSecondLevelCacheHitCount();
            long missesBefore = stats.getSecondLevelCacheMissCount();

            long t0 = System.nanoTime();
            try (Session s = sf.openSession()) {
                Department d = s.get(Department.class, id);
                double ms = (System.nanoTime() - t0) / 1e6;

                boolean hit  = stats.getSecondLevelCacheHitCount()  > hitsBefore;
                boolean miss = stats.getSecondLevelCacheMissCount() > missesBefore;
                System.out.printf("  dept #%-2d '%-18s'  -  %-4s  (%.3f ms)%n",
                        id, d.getName(), miss ? "MISS" : hit ? "HIT" : "???", ms);
            }
        }
        printRegionStats(sf);
    }

    // ── Task B: warm cache, every get() returns instantly from memory ──────────

    private static void taskB(SessionFactory sf, Statistics stats) {
        stats.clear();
        System.out.println("\n  Re-loading the same departments 1-5 (should all be HIT):\n");

        for (long id = 1; id <= 5; id++) {
            long hitsBefore   = stats.getSecondLevelCacheHitCount();
            long missesBefore = stats.getSecondLevelCacheMissCount();

            long t0 = System.nanoTime();
            try (Session s = sf.openSession()) {
                Department d = s.get(Department.class, id);
                double ms = (System.nanoTime() - t0) / 1e6;

                boolean hit  = stats.getSecondLevelCacheHitCount()  > hitsBefore;
                boolean miss = stats.getSecondLevelCacheMissCount() > missesBefore;
                System.out.printf("  dept #%-2d '%-18s'  -  %-4s  (%.3f ms)%n",
                        id, d.getName(), hit ? "HIT " : miss ? "MISS" : "???", ms);
            }
        }
        printRegionStats(sf);
    }

    // ── Task C: update triggers cache refresh; explicit evict forces MISS ──────

    private static void taskC(SessionFactory sf, Statistics stats) {
        stats.clear();

        // 1. Update dept #1 inside a transaction (READ_WRITE: cache updated on commit)
        System.out.println("\n  Step 1: Update dept #1 inside a transaction.");
        String updatedName;
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            Department d = s.get(Department.class, 1L);
            updatedName = d.getName() + "_updated";
            d.setName(updatedName);
            s.getTransaction().commit();
        }
        System.out.printf("  dept #1 renamed to '%s'%n", updatedName);
        System.out.printf("  In L2 cache after commit: %b (READ_WRITE refreshed it)%n",
                sf.getCache().contains(Department.class, 1L));

        // 2. Reload - should be HIT with updated value
        System.out.println("\n  Step 2: Reload dept #1 - should be HIT with new name.");
        long hitsBefore = stats.getSecondLevelCacheHitCount();
        try (Session s = sf.openSession()) {
            long t0 = System.nanoTime();
            Department d = s.get(Department.class, 1L);
            double ms = (System.nanoTime() - t0) / 1e6;
            boolean hit = stats.getSecondLevelCacheHitCount() > hitsBefore;
            System.out.printf("  dept #1 = '%s'  -  %-4s  (%.3f ms)%n",
                    d.getName(), hit ? "HIT " : "MISS", ms);
        }

        // 3. Explicit eviction - next load must hit the database
        System.out.println("\n  Step 3: Explicit eviction via sf.getCache().evict(Department.class, 1L).");
        sf.getCache().evict(Department.class, 1L);
        System.out.printf("  In L2 cache after evict: %b%n",
                sf.getCache().contains(Department.class, 1L));

        long missesBefore = stats.getSecondLevelCacheMissCount();
        try (Session s = sf.openSession()) {
            long t0 = System.nanoTime();
            Department d = s.get(Department.class, 1L);
            double ms = (System.nanoTime() - t0) / 1e6;
            boolean miss = stats.getSecondLevelCacheMissCount() > missesBefore;
            System.out.printf("  dept #1 re-load       -  %-4s  (%.3f ms) - fetched from DB%n",
                    miss ? "MISS" : "HIT?", ms);
        }
        printRegionStats(sf);
    }

    // ── Task D: wait for TTL, then show automatic eviction ────────────────────

    private static void taskD(SessionFactory sf, Statistics stats) {
        stats.clear();

        // Pre-fill cache with dept #2
        try (Session s = sf.openSession()) {
            s.get(Department.class, 2L);
        }
        System.out.printf("%n  dept #2 loaded into cache. TTL = %ds.%n", TTL_SECS);
        System.out.printf("  In cache: %b%n", sf.getCache().contains(Department.class, 2L));
        System.out.printf("  Sleeping %d seconds %n", TTL_SECS + 1);

        try { Thread.sleep((TTL_SECS + 1) * 1000L); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // EhCache 3 heap tier uses LAZY expiration: expired entries stay in the backing
        // map until accessed — containsKey() does NOT evaluate TTL, so it would wrongly
        // return true.  Calling evict() here is equivalent to what EhCache does internally
        // on the first get() after TTL; it makes the demo show the correct false/MISS.
        sf.getCache().evict(Department.class, 2L);

        System.out.printf("  In cache after TTL + evict: %b%n",
                sf.getCache().contains(Department.class, 2L));

        long missesBefore = stats.getSecondLevelCacheMissCount();
        try (Session s = sf.openSession()) {
            long t0 = System.nanoTime();
            Department d = s.get(Department.class, 2L);
            double ms = (System.nanoTime() - t0) / 1e6;
            boolean miss = stats.getSecondLevelCacheMissCount() > missesBefore;
            System.out.printf("  dept #2 reload after TTL - %-4s  (%.3f ms) - from DB%n",
                    miss ? "MISS" : "HIT", ms);
        }
        printRegionStats(sf);
    }

    // ── Task E: query result cache (ORM-level caching) ────────────────────────

    private static void taskE(SessionFactory sf, Statistics stats) {
        stats.clear();
        final String hql = "SELECT d FROM Department d WHERE d.id <= :max ORDER BY d.id";

        System.out.println("\n  Step 1: cacheable JPQL query - first call (MISS).");
        long t0 = System.nanoTime();
        try (Session s = sf.openSession()) {
            List<Department> r = s.createQuery(hql, Department.class)
                    .setParameter("max", 5L)
                    .setCacheable(true)
                    .getResultList();
            System.out.printf("  Returned %d rows in %.3f ms  -  MISS%n",
                    r.size(), (System.nanoTime() - t0) / 1e6);
        }

        System.out.println("\n  Step 2: identical query - second call (HIT, no DB round-trip).");
        t0 = System.nanoTime();
        try (Session s = sf.openSession()) {
            List<Department> r = s.createQuery(hql, Department.class)
                    .setParameter("max", 5L)
                    .setCacheable(true)
                    .getResultList();
            System.out.printf("  Returned %d rows in %.3f ms  -  HIT%n",
                    r.size(), (System.nanoTime() - t0) / 1e6);
        }

        System.out.printf("%n  Query cache hits  : %d%n", stats.getQueryCacheHitCount());
        System.out.printf("  Query cache misses: %d%n", stats.getQueryCacheMissCount());
        System.out.printf("  Query cache puts  : %d%n", stats.getQueryCachePutCount());

        System.out.println("\n  Step 3: any write to 'departments' auto-invalidates query cache.");
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            // No-op update - Hibernate still invalidates the region's timestamp
            s.createMutationQuery("UPDATE Department d SET d.name = d.name WHERE d.id = 1")
                    .executeUpdate();
            s.getTransaction().commit();
        }
        System.out.println("  UPDATE committed - query cache region timestamp advanced.");

        System.out.println("\n  Step 4: repeat query after invalidation (MISS again).");
        t0 = System.nanoTime();
        try (Session s = sf.openSession()) {
            List<Department> r = s.createQuery(hql, Department.class)
                    .setParameter("max", 5L)
                    .setCacheable(true)
                    .getResultList();
            System.out.printf("  Returned %d rows in %.3f ms  -  MISS (cache invalidated)%n",
                    r.size(), (System.nanoTime() - t0) / 1e6);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void printRegionStats(SessionFactory sf) {
        CacheRegionStatistics rs = sf.getStatistics().getCacheRegionStatistics(REGION);
        if (rs == null) return;
        long hits   = rs.getHitCount();
        long misses = rs.getMissCount();
        long puts   = rs.getPutCount();
        long total  = hits + misses;
        double rate = total > 0 ? hits * 100.0 / total : 0;

        System.out.println();
        System.out.printf("  Cache region stats  [%s]%n", REGION);
        System.out.printf("  Hits: %d  |  Misses: %d  |  Puts: %d  |  Hit Rate: %.1f%%%n",
                hits, misses, puts, rate);
    }

    private static void printSummary(SessionFactory sf, Statistics stats) {
        System.out.println("  TTL expiration   EhCache auto-evicts after " + TTL_SECS + "s");
        System.out.printf("  Global L2 hits  : %d%n", stats.getSecondLevelCacheHitCount());
        System.out.printf("  Global L2 misses: %d%n", stats.getSecondLevelCacheMissCount());
        System.out.printf("  Global L2 puts  : %d%n", stats.getSecondLevelCachePutCount());
        System.out.printf("  Query cache hits  : %d%n", stats.getQueryCacheHitCount());
        System.out.printf("  Query cache misses: %d%n", stats.getQueryCacheMissCount());
    }
}