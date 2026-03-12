package org.example.transpaletiiapp.service;


import org.example.transpaletiiapp.domain.Bearing;
import org.example.transpaletiiapp.domain.utils.validation.Validator;
import org.example.transpaletiiapp.repository.BearingRepository;

import java.util.UUID;

public class BearingsService extends IdentifiableService<UUID, Bearing> {
    public BearingsService(BearingRepository repository, Validator<Bearing> validator) {
        super(repository,validator);
    }
}
