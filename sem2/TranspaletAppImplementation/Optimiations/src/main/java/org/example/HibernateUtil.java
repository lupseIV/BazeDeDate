package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    // Configuration() automatically picks up hibernate.properties from the classpath root.
    private static final SessionFactory SESSION_FACTORY =
            new Configuration()
                    .addAnnotatedClass(Department.class)
                    .addAnnotatedClass(Employee.class)
                    .buildSessionFactory();

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void close() {
        SESSION_FACTORY.close();
    }
}
