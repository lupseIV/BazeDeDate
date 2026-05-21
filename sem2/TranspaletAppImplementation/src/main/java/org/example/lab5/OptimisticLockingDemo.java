package org.example.lab5;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Bearing;
import org.example.repository.BearingRepository;
import org.example.repository.jpa.BearingJpaRepository;
import org.example.repository.utils.JpaUtils;

import java.util.Scanner;
import java.util.UUID;

public class OptimisticLockingDemo {

    private static final Logger logger = LogManager.getLogger(OptimisticLockingDemo.class);
    private static final BearingRepository repo = new BearingJpaRepository(JpaUtils.getEntityManagerFactory());
    private static Bearing b = new Bearing(null, 11.0, null);

    public static void main(String[] args) {
        b = repo.save(b);
        runDemo();
    }

    public static void runDemo() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n--- SIMULARE CONCURENTA: LOCKING OPTIMIST ---");

        Bearing bearingUserA = repo.findById(b.getId()).orElseThrow();
        System.out.println("Utilizatorul A a incarcat Bearing cu versiunea: " + bearingUserA.getVersion());

        Bearing bearingUserB = repo.findById(b.getId()).orElseThrow();
        System.out.println("Utilizatorul B a incarcat Bearing cu versiunea: " + bearingUserB.getVersion());

        System.out.println("\nUtilizatorul A modifica diametrul la 100...");
        bearingUserA.setDiameter(100.0);
        // FIX: reassign so the returned entity (with the updated version) is captured
        bearingUserA = repo.save(bearingUserA);
        System.out.println("Utilizatorul A a salvat cu succes! Noua versiune in BD este: " + bearingUserA.getVersion());

        // Utilizatorul B incearca sa actualizeze cu versiune invechita (inca are version = 1)
        System.out.println("\nUtilizatorul B incearca sa modifice diametrul la 200...");
        bearingUserB.setDiameter(200.0);

        try {
            repo.save(bearingUserB);

        } catch (RollbackException e) {
            // FIX: Hibernate wraps OptimisticLockException inside RollbackException at transaction commit.
            // We must unwrap the cause to detect the conflict and show the resolution menu.
            if (e.getCause() instanceof OptimisticLockException) {
                logger.warn("CONFLICT DE VERSIUNE detectat pentru Bearing ID: {}. "
                        + "Utilizatorul B a incercat sa salveze versiunea {}, dar in BD versiunea este mai noua.",
                        b.getId(), bearingUserB.getVersion());

                System.err.println("\n[EROARE] Datele au fost modificate de un alt utilizator !");
                System.out.println("Alege o optiune pentru a rezolva conflictul:");
                System.out.println("  1 - Reincarcarea datelor (pastreaza modificarile celuilalt utilizator)");
                System.out.println("  2 - Actualizare fortata (suprascrie cu modificarile tale)");
                System.out.println("  3 - Anulare (renunta la tot)");
                System.out.print("Optiunea ta: ");

                int option = scanner.nextInt();

                switch (option) {
                    case 1:
                        Bearing reloadedBearing = repo.findById(b.getId()).orElseThrow();
                        System.out.println("Date reincarcate. Diametrul actual este: " + reloadedBearing.getDiameter());
                        break;
                    case 2:
                        Bearing freshBearing = repo.findById(b.getId()).orElseThrow();
                        freshBearing.setDiameter(bearingUserB.getDiameter());
                        repo.save(freshBearing);
                        System.out.println("Actualizare fortata reusita! Diametrul a fost suprascris.");
                        break;
                    case 3:
                        System.out.println("Operatiune anulata de utilizator.");
                        break;
                    default:
                        System.out.println("Optiune invalida. Anulare...");
                }
            } else {
                logger.error("Eroare la salvarea Bearing ID: {}. Detaliile erorii: {}", b.getId(), e.getMessage());
                System.err.println("\n[EROARE] A aparut o problema la salvarea datelor. Te rugam sa incerci din nou.");
            }
        } catch (OptimisticLockException e) {
            // Fallback: catch direct OptimisticLockException (some JPA providers throw it unwrapped)
            logger.warn("CONFLICT DE VERSIUNE (direct) detectat pentru Bearing ID: {}", b.getId());
            System.err.println("\n[EROARE] Conflict de versiune detectat. Datele au fost modificate de un alt utilizator.");
        }
    }
}