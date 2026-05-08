package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.List;

public class NplusOneDemo {

    public static void run() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Statistics stats = sf.getStatistics();

        System.out.println("  TASK A - n+1");
        long timeA = taskA(sf, stats);

        System.out.println("  TASK B - Solved with LEFT JOIN FETCH");
        long timeB = taskB(sf, stats);

        printSummary(timeA, timeB);
    }


    private static long taskA(SessionFactory sf, Statistics stats) {
        stats.clear();

        long start = System.currentTimeMillis();

        try (Session session = sf.openSession()) {
            List<Department> departments = session
                    .createQuery("SELECT d FROM Department d", Department.class)
                    .getResultList();

            System.out.printf("Loaded %d Department rows with 1 query", departments.size());

            for (Department dept : departments) {
                List<Employee> employees = dept.getEmployees();
                System.out.printf("   %-28s  -  %d employees%n",
                        dept.getName(), employees.size());
            }
        }

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\nTask A  stats");
        System.out.println("Prepared SQL statements : " + stats.getPrepareStatementCount());
        System.out.println("JPQL queries executed   : " + stats.getQueryExecutionCount());
        System.out.println("Entities loaded         : " + stats.getEntityLoadCount());
        System.out.println("Execution time          : " + elapsed + " ms");

        return elapsed;
    }


    private static long taskB(SessionFactory sf, Statistics stats) {
        stats.clear();

        long start = System.currentTimeMillis();

        try (Session session = sf.openSession()) {
            List<Department> departments = session
                    .createQuery(
                            "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees",
                            Department.class)
                    .getResultList();

            System.out.printf("Loaded %d Department rows WITH their employees (1 query)",
                    departments.size());

            for (Department dept : departments) {
                List<Employee> employees = dept.getEmployees();
                System.out.printf("   %-28s  -  %d employees%n",
                        dept.getName(), employees.size());
            }
        }

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\nTask B  stats");
        System.out.println("Prepared SQL statements : " + stats.getPrepareStatementCount());
        System.out.println("JPQL queries executed   : " + stats.getQueryExecutionCount());
        System.out.println("Entities loaded         : " + stats.getEntityLoadCount());
        System.out.println("Execution time          : " + elapsed + " ms");

        return elapsed;
    }

    private static void printSummary(long timeA, long timeB) {
        System.out.println("  N+1 summary");
        System.out.println("=".repeat(65));
        System.out.printf("%-32s  %-16s  %s%n", "Approach", "SQL Statements", "Time (ms)");
        System.out.println("-".repeat(65));
        System.out.printf("%-32s  %-16s  %d%n", "Task A - N+1",        "1 + N",  timeA);
        System.out.printf("%-32s  %-16s  %d%n", "Task B - JOIN FETCH", "1",      timeB);
        System.out.println("-".repeat(65));
    }
}
