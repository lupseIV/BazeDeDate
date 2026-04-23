package org.example.repository.utils;

import jakarta.persistence.EntityManagerFactory;

public final class JpaUtils {

    private static final String PERSISTENCE_UNIT_NAME = "ProiectPU";
    private static EntityManagerFactory entityManagerFactory = null;

    private JpaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            entityManagerFactory = jakarta.persistence.Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
        return entityManagerFactory;
    }

}
