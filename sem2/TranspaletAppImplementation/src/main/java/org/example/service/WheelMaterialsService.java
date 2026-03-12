package org.example.service;

import org.example.domain.WheelMaterial;
import org.example.domain.utils.validation.Validator;
import org.example.repository.WheelMaterialRepository;
import org.example.repository.WheelsRepository;

import java.util.UUID;

public class WheelMaterialsService extends IdentifiableService<UUID, WheelMaterial> {
    public WheelMaterialsService(WheelMaterialRepository repository, Validator<WheelMaterial> validator) {
        super(repository, validator);
    }
}
