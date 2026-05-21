package org.example.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Rental;
import org.example.repository.RentalRepository;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RentalJpaRepository implements RentalRepository {

    private static final Logger logger = LogManager.getLogger(RentalJpaRepository.class);
    private final EntityManagerFactory entityManagerFactory;

    public RentalJpaRepository(EntityManagerFactory emf) {
        this.entityManagerFactory = emf;
    }

    @Override
    public Rental save(Rental entity) {
        logger.info("Saving rental {}", entity);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                if (entity.getId() == null) {
                    em.persist(entity);
                } else {
                    entity = em.merge(entity);
                }
                em.getTransaction().commit();
                return entity;
            } catch (Exception e) {
                logger.error("Error saving rental {}: {}", entity, e.getMessage());
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw e;
            }
        }
    }

    @Override
    public Optional<Rental> findById(UUID uuid) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            // Using JOIN FETCH to initialize the lazy PalletTruck proxy
            List<Rental> results = em.createQuery(
                            "SELECT r FROM Rental r JOIN FETCH r.truck WHERE r.id = :id", Rental.class)
                    .setParameter("id", uuid)
                    .getResultList();
            return results.stream().findFirst();
        }
    }

    @Override
    public List<Rental> findAll() {
        logger.info("Finding all rentals");
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            Session session = em.unwrap(Session.class);
            session.enableFilter("deletedFilter").setParameter("isDeleted", false);
            return em.createQuery("SELECT r FROM Rental r JOIN FETCH r.truck", Rental.class).getResultList();
        }
    }

    @Override
    public List<Rental> findAllInlcudingDeleted() {
        logger.info("Finding all rentals including deleted");
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            return em.createQuery("SELECT r FROM Rental r JOIN FETCH r.truck", Rental.class).getResultList();
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Rental rental = em.find(Rental.class, uuid);
                if (rental != null) {
                    em.remove(rental);
                    em.getTransaction().commit();
                } else {
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw e;
            }
        }
    }

    @Override
    public void delete(UUID id, boolean hard) {
        if(!hard){
            deleteById(id);
        } else {
            hardDeleteById(id);
        }
    }

    private void hardDeleteById(UUID uuid){
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                int deletedCount = em.createQuery("DELETE FROM Rental r WHERE r.id = :id")
                        .setParameter("id", uuid)
                        .executeUpdate();
                if (deletedCount == 0) {
                    logger.warn("Nu a fost gasită nicio inregistrare cu ID-ul {} pentru stergere fizica.", uuid);
                }
                em.getTransaction().commit();
                em.clear();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw e;
            }
        }
    }

    @Override
    public void restoreById(UUID uuid) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Rental rental = em.find(Rental.class, uuid);
                if (rental != null && rental.isDeleted()) {
                    rental.setDeleted(false);
                    rental.setDeletedAt(null);
                    rental.setDeletedBy(null);
                    em.getTransaction().commit();
                } else {
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw e;
            }
        }
    }
}