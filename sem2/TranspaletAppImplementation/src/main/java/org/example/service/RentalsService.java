package org.example.service;

import org.example.domain.Rental;
import org.example.domain.utils.validation.Validator;
import org.example.repository.RentalRepository;

import java.util.List;
import java.util.UUID;

public class RentalsService extends IdentifiableService<UUID, Rental> {

    private RentalRepository repo;

    public RentalsService(RentalRepository repository, Validator<Rental> validator) {
        super(repository, validator);
        this.repo = repository;
    }

    public void delete(UUID id , boolean hard) {
        this.repo.delete(id,  hard);
    }

    public void restoreById(UUID id) {
        this.repo.restoreById(id);
    }

    public List<Rental> findAllIncludingDeleted() {
        return repo.findAllInlcudingDeleted();
    }
}