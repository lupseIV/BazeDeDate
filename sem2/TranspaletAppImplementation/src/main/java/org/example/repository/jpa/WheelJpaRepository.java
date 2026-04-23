package org.example.repository.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Wheel;
import org.example.repository.WheelsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WheelJpaRepository implements WheelsRepository {

    private static final Logger logger = LogManager.getLogger(WheelJpaRepository.class);
    private final EntityManagerFactory entityManagerFactory;

    public WheelJpaRepository(EntityManagerFactory emf) {
        this.entityManagerFactory = emf;
    }

    @Override
    public Wheel save(Wheel entity) {
        logger.info("Saving wheel {}", entity);

        try (var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();

            try {
                if (entity.getId() == null) {
                    logger.info("Creating wheel {}", entity);
                    em.persist(entity);
                } else {
                    logger.info("Updating wheel {}", entity);
                    entity = em.merge(entity);
                }

                em.getTransaction().commit();
                logger.info("Wheel {} committed to database", entity);
                return entity;

            } catch (Exception e) {
                logger.error("Error saving wheel {}: {}", entity, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for wheel {}", entity);
                }
                throw e;
            }
        }
    }

    @Override
    public Optional<Wheel> findById(UUID uuid) {
        logger.info("Finding wheel {}", uuid);
        try(var em = entityManagerFactory.createEntityManager()) {
            Wheel wheel = em.find(Wheel.class, uuid);
            if(wheel != null) {
                logger.info("Wheel {} found", wheel);
            } else {
                logger.info("Wheel with id {} not found", uuid);
            }
            return Optional.ofNullable(wheel);
        } catch (Exception e) {
            logger.error("Error finding wheel with id {}: {}", uuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Wheel> findAll() {
        logger.info("Finding all wheels");
        try(var em = entityManagerFactory.createEntityManager()) {
            List<Wheel> wheels = em.createQuery("SELECT w FROM Wheel w JOIN fetch w.material JOIN fetch w.bearing", Wheel.class).getResultList();
            logger.info("{} wheels found", wheels.size());
            return wheels;
        } catch (Exception e) {
            logger.error("Error finding all wheels: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        logger.info("Deleting wheel {}", uuid);
        try(var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            Wheel wheel = em.find(Wheel.class, uuid);
            try{
                if(wheel != null) {
                    em.remove(wheel);
                    logger.info("Wheel {} deleted", wheel);
                } else {
                    logger.warn("Wheel with id {} not found for deletion", uuid);
                }
                em.getTransaction().commit();
            } catch (Exception e) {
                logger.error("Error deleting wheel with id {}: {}", uuid, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for wheel deletion with id {}", uuid);
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error deleting wheel with id {}: {}", uuid, e.getMessage());

            throw e;
        }
    }
}
