package org.example.service;

import org.example.domain.PalletTruck;
import org.example.domain.utils.validation.Validator;
import org.example.repository.PalletTruckRepository;

import java.util.UUID;

public class PalletTrucksService extends IdentifiableService<UUID, PalletTruck> {
    public PalletTrucksService(PalletTruckRepository repository, Validator<PalletTruck> validator) {
        super(repository, validator);
    }
}
