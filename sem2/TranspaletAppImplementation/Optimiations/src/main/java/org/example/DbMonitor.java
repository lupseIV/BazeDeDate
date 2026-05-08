package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class DbMonitor {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Full report ───────────────────────────────────────────────────────────

    public static void printFullReport(SessionFactory sf) {
        Statistics stats = sf.getStatistics();
        System.out.printf("  DB MONITOR REPORT  [%s]%n", LocalDateTime.now().format(FMT));
        printSessionMetrics(stats);
        printEntityMetrics(stats);
        printCacheMetrics(sf, stats);
        printQueryMetrics(stats);
        System.out.printf(Arrays.toString(snapshot(stats)));
    }

    // ── Section printers ──────────────────────────────────────────────────────

    public static void printSessionMetrics(Statistics stats) {
        System.out.println("\n  [Session & Transaction]");
        row("Sessions opened",      stats.getSessionOpenCount());
        row("Sessions closed",      stats.getSessionCloseCount());
        row("Transactions",         stats.getTransactionCount());
        row("Successful TX",        stats.getSuccessfulTransactionCount());
        row("Flushes",              stats.getFlushCount());
        row("Connections obtained", stats.getConnectCount());
        row("Statements prepared",  stats.getPrepareStatementCount());
        row("Statements closed",    stats.getCloseStatementCount());
    }

    public static void printEntityMetrics(Statistics stats) {
        System.out.println("\n  [Entity Operations]");
        row("Loads",   stats.getEntityLoadCount());
        row("Fetches", stats.getEntityFetchCount());
        row("Inserts", stats.getEntityInsertCount());
        row("Updates", stats.getEntityUpdateCount());
        row("Deletes", stats.getEntityDeleteCount());
    }

    public static void printCacheMetrics(SessionFactory sf, Statistics stats) {
        System.out.println("\n  [L2 Entity Cache – Global]");
        long h = stats.getSecondLevelCacheHitCount();
        long m = stats.getSecondLevelCacheMissCount();
        long t = h + m;
        row("Hits",   h);
        row("Misses", m);
        row("Puts",   stats.getSecondLevelCachePutCount());
        System.out.printf("  %-30s %.1f%%%n", "Hit rate:", t > 0 ? h * 100.0 / t : 0);

        // Per-region details
        for (String region : stats.getSecondLevelCacheRegionNames()) {
            CacheRegionStatistics rs = stats.getCacheRegionStatistics(region);
            if (rs != null && (rs.getHitCount() + rs.getMissCount()) > 0) {
                System.out.printf("  Region %-24s  hits=%-5d  misses=%-5d  inMem=%d%n",
                        shorten(region) + ":", rs.getHitCount(), rs.getMissCount(),
                        rs.getElementCountInMemory());
            }
        }

        System.out.println("\n  [Query Result Cache]");
        long qh = stats.getQueryCacheHitCount();
        long qm = stats.getQueryCacheMissCount();
        long qt = qh + qm;
        row("Hits",   qh);
        row("Misses", qm);
        row("Puts",   stats.getQueryCachePutCount());
        System.out.printf("  %-30s %.1f%%%n", "Hit rate:", qt > 0 ? qh * 100.0 / qt : 0);
    }

    public static void printQueryMetrics(Statistics stats) {
        System.out.println("\n  [Query Execution]");
        row("Queries executed", stats.getQueryExecutionCount());
        System.out.printf("  %-30s %d ms%n", "Max query time:", stats.getQueryExecutionMaxTime());
        String slow = stats.getQueryExecutionMaxTimeQueryString();
        if (slow != null && !slow.isBlank()) {
            System.out.printf("  Slowest: %s%n",
                    slow.length() > 70 ? slow.substring(0, 70) + "…" : slow);
        }
    }

    public static long[] snapshot(Statistics stats) {
        return new long[]{
            stats.getSecondLevelCacheHitCount(),
            stats.getSecondLevelCacheMissCount(),
            stats.getSecondLevelCachePutCount(),
            stats.getQueryCacheHitCount(),
            stats.getQueryCacheMissCount(),
            stats.getEntityLoadCount(),
            stats.getPrepareStatementCount(),
            stats.getQueryExecutionCount()
        };
    }

    private static void row(String label, long value) {
        System.out.printf("  %-30s %d%n", label + ":", value);
    }

    private static String shorten(String region) {
        int dot = region.lastIndexOf('.');
        return dot >= 0 ? region.substring(dot + 1) : region;
    }
}