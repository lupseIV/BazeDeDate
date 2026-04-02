package org.example.lab2;

import java.util.Scanner;

public class Main {

     static void main() {
        Transactions concurrencyDemo = new Transactions();
        BatchPerformance batchDemo = new BatchPerformance();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n========== MENIU LABORATOR 2 ==========");
            System.out.println("1. Demonstratie A - Dirty Read");
            System.out.println("2. Demonstratie B - Non-Repeatable Read");
            System.out.println("3. Demonstratie C - Phantom Read");
            System.out.println("4. Demonstratie D - Lost Update");
            System.out.println("5. Demonstratie Deadlock");
            System.out.println("6. Testare Performanta Batch Insert");
            System.out.println("0. Iesire");
            System.out.print("Alege o optiune: ");

            int option = scanner.nextInt();

            try {
                switch (option) {
                    case 1: concurrencyDemo.demoDirtyRead(); break;
                    case 2: concurrencyDemo.demoNonRepeatableRead(); break;
                    case 3: concurrencyDemo.demoPhantomRead(); break;
                    case 4: concurrencyDemo.demoLostUpdate(); break;
                    case 5: concurrencyDemo.demoDeadlock(); break;
                    case 6: batchDemo.runPerformanceTests(); break;
                    case 0: System.exit(0);
                    default: System.out.println("Optiune invalida!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}