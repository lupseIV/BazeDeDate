package org.example.service;

import org.example.domain.Bearing;
import org.example.domain.utils.validation.Validator;
import org.example.repository.BearingRepository;

import java.util.UUID;

public class BearingsService extends IdentifiableService<UUID, Bearing> {
    public BearingsService(BearingRepository repository, Validator<Bearing> validator) {
        super(repository,validator);
    }
}
