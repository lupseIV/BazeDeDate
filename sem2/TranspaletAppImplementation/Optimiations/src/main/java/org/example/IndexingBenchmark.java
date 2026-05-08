package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Random;

public class IndexingBenchmark {

    private static final int  ITERATIONS = 100;

    private static final String Q1 = "SELECT * FROM employees WHERE email = :p1";
    private static final String Q2 = "SELECT * FROM employees WHERE department_id = :p1";
    private static final String Q3 = "SELECT * FROM employees WHERE salary BETWEEN :p1 AND :p2";
    private static final String Q4 = "SELECT * FROM employees WHERE department_id = :p1 AND salary > :p2";

    public static void run() {
        SessionFactory sf = HibernateUtil.getSessionFactory();

        Long deptId = 5L;
        String sampleEmail = "user_200@example.com";


        System.out.println("  TASK A - Benchmark WITHOUT Indexes");
        dropIndexes(sf);
        double[] timesWithout = benchmark(sf, deptId, sampleEmail, "Task A");

        System.out.println("\n EXPLAIN ANALYZE without indexes");
        runExplainAnalyze(sf, deptId, sampleEmail);


        System.out.println("  TASK B - Creating Indexes");
        createIndexes(sf);


        System.out.println("  TASK C - Re-benchmark WITH Indexes");
        double[] timesWith = benchmark(sf, deptId, sampleEmail, "Task C");

        System.out.println("\nEXPLAIN ANALYZE with indexes");
        runExplainAnalyze(sf, deptId, sampleEmail);

        printComparison(timesWithout, timesWith);
    }


    private static void dropIndexes(SessionFactory sf) {
        String[] ddl = {
            "DROP INDEX IF EXISTS idx_employees_email",
            "DROP INDEX IF EXISTS idx_employees_department_id",
            "DROP INDEX IF EXISTS idx_employees_salary",
            "DROP INDEX IF EXISTS idx_employees_dept_salary"
        };
        executeDDL(sf, ddl);
    }

    private static void createIndexes(SessionFactory sf) {
        String[] ddl = {
            "CREATE INDEX IF NOT EXISTS idx_employees_email          ON employees(email)",
            "CREATE INDEX IF NOT EXISTS idx_employees_department_id  ON employees(department_id)",
            "CREATE INDEX IF NOT EXISTS idx_employees_salary         ON employees(salary)",
            "CREATE INDEX IF NOT EXISTS idx_employees_dept_salary    ON employees(department_id, salary)"
        };
        executeDDL(sf, ddl);
    }

    private static void executeDDL(SessionFactory sf, String[] statements) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            for (String sql : statements) {
                s.createNativeQuery(sql).executeUpdate();
                System.out.println("[DDL] " + sql.trim());
            }
            s.getTransaction().commit();
        }
    }

    private static double[] benchmark(SessionFactory sf, Long deptId, String email, String label) {
        System.out.printf("[%s] Running %d iterations per query", label, ITERATIONS);

        double[] results = {
            timeOne(sf, Q1, "p1", email),
            timeOne(sf, Q2, "p1", deptId),
            timeTwo(sf, Q3, "p1", 50_000.0, "p2", 80_000.0),
            timeTwo(sf, Q4, "p1", deptId,   "p2", 60_000.0)
        };

        String[] labels = {"Q1 email search         ",
                           "Q2 department search    ",
                           "Q3 salary range         ",
                           "Q4 multi-column         "};
        for (int i = 0; i < 4; i++) {
            System.out.printf("  %s : %8.3f ms avg%n", labels[i], results[i]);
        }
        return results;
    }

    private static double timeOne(SessionFactory sf, String sql, String p1, Object v1) {
        long total = 0;
        try (StatelessSession ss = sf.openStatelessSession()) {
            for (int i = 0; i < ITERATIONS; i++) {
                long t0 = System.nanoTime();
                ss.createNativeQuery(sql).setParameter(p1, v1).getResultList();
                total += System.nanoTime() - t0;
            }
        }
        return total / (double) ITERATIONS / 1_000_000.0;
    }

    private static double timeTwo(SessionFactory sf, String sql,
                                  String p1, Object v1, String p2, Object v2) {
        long total = 0;
        try (StatelessSession ss = sf.openStatelessSession()) {
            for (int i = 0; i < ITERATIONS; i++) {
                long t0 = System.nanoTime();
                ss.createNativeQuery(sql).setParameter(p1, v1).setParameter(p2, v2).getResultList();
                total += System.nanoTime() - t0;
            }
        }
        return total / (double) ITERATIONS / 1_000_000.0;
    }

    private static void runExplainAnalyze(SessionFactory sf, Long deptId, String email) {
        String[] queryLabels = {
            "Q1 - email search",
            "Q2 - department_id search",
            "Q3 - salary BETWEEN 50000 AND 80000",
            "Q4 - department_id + salary > 60000 (multi-column)"
        };
        String[] explainSql = {
            "EXPLAIN ANALYZE SELECT * FROM employees WHERE email = '" + email + "'",
            "EXPLAIN ANALYZE SELECT * FROM employees WHERE department_id = " + deptId,
            "EXPLAIN ANALYZE SELECT * FROM employees WHERE salary BETWEEN 50000 AND 80000",
            "EXPLAIN ANALYZE SELECT * FROM employees WHERE department_id = " + deptId + " AND salary > 60000"
        };

        try (Session session = sf.openSession()) {
            for (int i = 0; i < explainSql.length; i++) {
                System.out.println("\n>> " + queryLabels[i]);
                @SuppressWarnings("unchecked")
                List<String> plan = session.createNativeQuery(explainSql[i], String.class)
                        .getResultList();
                plan.stream().limit(6).forEach(line -> System.out.println("   " + line));
            }
        }
    }

    private static void printComparison(double[] without, double[] with) {
        String[] labels = {
            "Email search",
            "Department search",
            "Salary range",
            "Multi-column (dept+salary)"
        };

        System.out.println("  INDEXING COMPARISON  (" + ITERATIONS + " iterations each)");
        System.out.printf("%-28s  %14s  %14s  %12s%n",
                "Query", "Without Index", "With Index", "Improvement");
        System.out.println("-".repeat(75));

        for (int i = 0; i < 4; i++) {
            double factor = (with[i] > 0) ? without[i] / with[i] : 0;
            System.out.printf("%-28s  %11.3f ms  %11.3f ms  %9.1fx%n",
                    labels[i], without[i], with[i], factor);
        }
    }
}
