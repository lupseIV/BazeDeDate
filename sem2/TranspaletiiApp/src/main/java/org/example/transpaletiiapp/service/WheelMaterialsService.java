package org.example.transpaletiiapp.service;


import org.example.transpaletiiapp.domain.WheelMaterial;
import org.example.transpaletiiapp.domain.utils.validation.Validator;
import org.example.transpaletiiapp.repository.WheelMaterialRepository;

import java.util.UUID;

public class WheelMaterialsService extends IdentifiableService<UUID, WheelMaterial> {
    public WheelMaterialsService(WheelMaterialRepository repository, Validator<WheelMaterial> validator) {
        super(repository, validator);
    }
}
