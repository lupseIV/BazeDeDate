package org.example.repository;

import org.example.domain.Rental;

import java.util.List;
import java.util.UUID;

public interface RentalRepository extends CrudRepository<UUID, Rental> {
   List<Rental> findAllInlcudingDeleted();
   void delete(UUID id, boolean hard);
   void restoreById(UUID id);
}