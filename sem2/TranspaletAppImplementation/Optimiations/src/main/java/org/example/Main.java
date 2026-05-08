package org.example;

public class Main {
    public static void main(String[] args) {

        try {
//            PaginationUI.launch();

//            HealthCheck.run();
//            NplusOneDemo.run();
//            IndexingBenchmark.run();
//            PaginationsTest.run();
//            CachingDemo.run();              // bonus
//            BulkUpdateBenchmark.run();
//            PreparedStatementCachingDemo.run();
//            DbMonitor.printFullReport(HibernateUtil.getSessionFactory());   // bonus
            PerformanceSuite.run();               // runs all + writes performance_report.txt (bonus)
        } finally {
            HibernateUtil.close();
        }
    }
}
