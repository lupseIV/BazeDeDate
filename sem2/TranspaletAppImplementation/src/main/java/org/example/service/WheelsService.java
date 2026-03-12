package org.example.service;

import org.example.domain.Wheel;
import org.example.domain.utils.validation.Validator;
import org.example.repository.WheelsRepository;

import java.util.UUID;

public class WheelsService extends IdentifiableService<UUID, Wheel> {
    public WheelsService(WheelsRepository repository, Validator<Wheel> validator) {
        super(repository, validator);
    }
}
