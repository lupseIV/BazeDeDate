package org.example.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.PalletTruck;
import org.example.repository.PalletTruckRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PalletTruckJpaRepository implements PalletTruckRepository {

    private static final Logger logger = LogManager.getLogger(PalletTruckJpaRepository.class);
    private final EntityManagerFactory entityManagerFactory;

    public PalletTruckJpaRepository(EntityManagerFactory emf) {
        this.entityManagerFactory = emf;
    }

    @Override
    public PalletTruck save(PalletTruck entity) {
        logger.info("Saving pallet truck {}", entity);

        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();

            try {
                if (entity.getId() == null) {
                    logger.info("Creating pallet truck {}", entity);
                    em.persist(entity);
                } else {
                    logger.info("Updating pallet truck {}", entity);
                    entity = em.merge(entity);
                }

                em.getTransaction().commit();
                logger.info("Pallet truck {} committed to database", entity);
                return entity;

            } catch (Exception e) {
                logger.error("Error saving pallet truck {}: {}", entity, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    logger.warn("Transaction rolled back for pallet truck {}", entity);
                }
                throw e;
            }
        }
    }

    @Override
    public Optional<PalletTruck> findById(UUID uuid) {
        logger.info("Finding pallet truck with id {}", uuid);

        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            PalletTruck truck = em.find(PalletTruck.class, uuid);
            if (truck != null) {
                logger.info("Pallet truck found: {}", truck);
                return Optional.of(truck);
            } else {
                logger.warn("Pallet truck with id {} not found", uuid);
                return Optional.empty();
            }
        }
    }

    @Override
    public List<PalletTruck> findAll() {
        logger.info("Finding all pallet trucks");

        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            return em.createQuery("SELECT p FROM PalletTruck p", PalletTruck.class).getResultList();
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        logger.info("Deleting pallet truck with id {}", uuid);

        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();

            try {
                PalletTruck truck = em.find(PalletTruck.class, uuid);
                if (truck != null) {
                    em.remove(truck);
                    em.getTransaction().commit();
                    logger.info("Pallet truck with id {} deleted", uuid);
                } else {
                    logger.warn("Pallet truck with id {} not found for deletion", uuid);
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
                logger.error("Error deleting pallet truck with id {}: {}", uuid, e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }
}
