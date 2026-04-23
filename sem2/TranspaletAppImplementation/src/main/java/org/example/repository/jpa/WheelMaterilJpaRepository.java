package org.example.repository.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.WheelMaterial;
import org.example.repository.WheelMaterialRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WheelMaterilJpaRepository implements WheelMaterialRepository {

    private static final Logger logger = LogManager.getLogger(WheelMaterilJpaRepository.class);
    private final EntityManagerFactory entityManagerFactory;

    public WheelMaterilJpaRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public WheelMaterial save(WheelMaterial entity) {
        logger.info("Saving wheel material {}", entity);

        try (var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();

            try {
                if (entity.getId() == null) {
                    logger.info("Creating wheel material {}", entity);
                    em.persist(entity);
                } else {
                    logger.info("Updating wheel material {}", entity);
                    entity = em.merge(entity);
                }

                em.getTransaction().commit();
                logger.info("Wheel material {} committed to database", entity);
                return entity;

            } catch (Exception e) {
                logger.error("Error saving wheel material {}: {}", entity, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for wheel material {}", entity);
                }
                throw e;
            }
        }
    }

    @Override
    public Optional<WheelMaterial> findById(UUID uuid) {
        logger.info("Finding wheel material by id {}", uuid);
        try (var em = entityManagerFactory.createEntityManager()) {
            WheelMaterial material = em.find(WheelMaterial.class, uuid);
            if (material != null) {
                logger.info("Wheel material found: {}", material);
                return Optional.of(material);
            } else {
                logger.info("No wheel material found with id {}", uuid);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error finding wheel material with id {}: {}", uuid, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<WheelMaterial> findAll() {
        logger.info("Finding all wheel material entities");
        try (var em = entityManagerFactory.createEntityManager()) {
            var query = em.createQuery("SELECT wm FROM WheelMaterial wm", WheelMaterial.class);
            List<WheelMaterial> materials = query.getResultList();
            logger.info("Found {} wheel material entities", materials.size());
            return materials;
        } catch (Exception e) {
            logger.error("Error finding all wheel material entities: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        logger.info("Deleting wheel material by id {}", uuid);
        try (var em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            WheelMaterial material = em.find(WheelMaterial.class, uuid);
            if (material != null) {
                em.remove(material);
                logger.info("Wheel material with id {} deleted", uuid);
            } else {
                logger.warn("No wheel material found with id {}, nothing to delete", uuid);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error deleting wheel material with id {}: {}", uuid, e.getMessage());
            throw e;
        }
    }
}
