package org.example.repository.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Bearing;
import org.example.repository.BearingRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class BearingJpaRepository implements BearingRepository {

    private static final Logger logger = LogManager.getLogger(BearingJpaRepository.class);
    private final EntityManagerFactory entityManagerFactory;

    public BearingJpaRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Bearing save(Bearing entity) {
        logger.info("Saving bearing with id {}", entity.getId());
        try(var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                if (entity.getId() == null) {
                    logger.info("Creating new bearing {}", entity);
                    em.persist(entity);
                } else {
                    logger.info("Updating existing bearing {}", entity);
                    entity = em.merge(entity);
                }
                em.getTransaction().commit();
                logger.info("Bearing {} committed to database", entity);
                return entity;
            } catch (Exception e) {
                logger.error("Error saving bearing {}: {}", entity, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for bearing {}", entity);
                }
                throw e;
            }
        }
    }

    @Override
    public Optional<Bearing> findById(UUID uuid) {
        logger.info("Finding bearing with id {}", uuid);
        try (var em = entityManagerFactory.createEntityManager()) {
            Bearing bearing = em.find(Bearing.class, uuid);
            if (bearing != null) {
                logger.info("Bearing found: {}", bearing);
            } else {
                logger.info("No bearing found with id {}", uuid);
            }
            return Optional.ofNullable(bearing);
        } catch (Exception e) {
            logger.error("Error finding bearing with id {}: {}", uuid, e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public List<Bearing> findAll() {
        logger.info("Finding all bearings");
        try (var em = entityManagerFactory.createEntityManager()) {
            List<Bearing> bearings = em.createQuery("SELECT b FROM Bearing b", Bearing.class).getResultList();
            logger.info("Found {} bearings", bearings.size());
            return bearings;
        } catch (Exception e) {
            logger.error("Error finding all bearings: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        logger.info("Deleting bearing with id {}", uuid);
        try (var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Bearing bearing = em.find(Bearing.class, uuid);
                if (bearing != null) {
                    em.remove(bearing);
                    logger.info("Bearing with id {} deleted", uuid);
                } else {
                    logger.warn("No bearing found with id {}, nothing to delete", uuid);
                }
                em.getTransaction().commit();
            } catch (Exception e) {
                logger.error("Error deleting bearing with id {}: {}", uuid, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for deleting bearing with id {}", uuid);
                }
                throw e;
            }
        }
    }
}
