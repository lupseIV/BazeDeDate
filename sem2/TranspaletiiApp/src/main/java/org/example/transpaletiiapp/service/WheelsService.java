package org.example.transpaletiiapp.service;


import org.example.transpaletiiapp.domain.Wheel;
import org.example.transpaletiiapp.domain.utils.validation.Validator;
import org.example.transpaletiiapp.repository.WheelsRepository;

import java.util.UUID;

public class WheelsService extends IdentifiableService<UUID, Wheel> {
    public WheelsService(WheelsRepository repository, Validator<Wheel> validator) {
        super(repository, validator);
    }
}
