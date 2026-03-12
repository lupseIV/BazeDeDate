package org.example.repository;

import org.example.domain.Identifiable;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<ID, E extends Identifiable<ID>> {
    E save(E entity);
    Optional<E> findById(ID id);
    List<E> findAll();
    void deleteById(ID id);
}
