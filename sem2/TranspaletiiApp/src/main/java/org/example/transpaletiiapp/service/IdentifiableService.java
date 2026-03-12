package org.example.transpaletiiapp.service;


import org.example.transpaletiiapp.domain.Identifiable;
import org.example.transpaletiiapp.domain.exceptions.ServiceException;
import org.example.transpaletiiapp.domain.utils.validation.Validator;
import org.example.transpaletiiapp.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public abstract class IdentifiableService<ID,E extends Identifiable<ID>> {

    private final CrudRepository<ID,E> repository;
    private final Validator<E> validator;

    public IdentifiableService(CrudRepository<ID,E> repository, Validator<E> validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public E save(E entity) {
        if(entity == null){
            throw new ServiceException("Null entity error.");
        }
        validator.validate(entity);
        return repository.save(entity);
    }


    public Optional<E> findById(ID id) {

        if (id == null) {
            throw new ServiceException("Null id error.");
        }
        return repository.findById(id);
    }

    public List<E> findAll() {
        return repository.findAll();
    }

    public void deleteById(ID id) {
        if (id == null) {
            throw new ServiceException("Null id error.");
        }
        if(repository.findById(id).isEmpty()){
            throw new ServiceException("Entity with id " + id + " does not exist.");
        }
        repository.deleteById(id);
    }
}
