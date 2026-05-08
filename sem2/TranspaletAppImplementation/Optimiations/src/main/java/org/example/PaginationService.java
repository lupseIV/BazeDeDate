package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.List;

public class PaginationService {

    private final SessionFactory sf;

    public PaginationService(SessionFactory sf) {
        this.sf = sf;
    }

    // ── Strategy A: Offset

    public Page<Employee> getPage(int pageNumber, int pageSize) {
        try (Session session = sf.openSession()) {
            List<Employee> employees = session
                    .createQuery("SELECT e FROM Employee e ORDER BY e.id", Employee.class)
                    .setFirstResult(pageNumber * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();

            long total = session
                    .createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                    .getSingleResult();

            return new Page<>(employees, pageNumber, pageSize, total);
        }
    }

    // ── Strategy B: Keyset

    public Page<Employee> getPageAfter(Long lastId, int pageSize) {
        try (Session session = sf.openSession()) {
            // Fetch one extra row to detect whether a next page exists.
            List<Employee> raw = session
                    .createQuery(
                            "SELECT e FROM Employee e WHERE e.id > :lastId ORDER BY e.id",
                            Employee.class)
                    .setParameter("lastId", lastId)
                    .setMaxResults(pageSize + 1)
                    .getResultList();

            boolean hasNext = raw.size() > pageSize;
            List<Employee> employees = hasNext ? new ArrayList<>(raw.subList(0, pageSize)) : raw;
            Long newLastId = employees.isEmpty() ? lastId : employees.get(employees.size() - 1).getId();

            return new Page<>(employees, pageSize, newLastId, hasNext);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    public long countAll() {
        try (Session session = sf.openSession()) {
            return session
                    .createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                    .getSingleResult();
        }
    }

    Long idAtOffset(int rowIndex) {
        try (Session session = sf.openSession()) {
            List<Long> ids = session
                    .createQuery("SELECT e.id FROM Employee e ORDER BY e.id", Long.class)
                    .setFirstResult(rowIndex)
                    .setMaxResults(1)
                    .getResultList();
            return ids.isEmpty() ? 0L : ids.get(0);
        }
    }
}
