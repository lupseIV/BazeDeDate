package org.example.transpaletiiapp.service;


import org.example.transpaletiiapp.domain.PalletTruck;
import org.example.transpaletiiapp.domain.utils.validation.Validator;
import org.example.transpaletiiapp.repository.PalletTruckRepository;

import java.util.UUID;

public class PalletTrucksService extends IdentifiableService<UUID, PalletTruck> {
    public PalletTrucksService(PalletTruckRepository repository, Validator<PalletTruck> validator) {
        super(repository, validator);
    }
}
