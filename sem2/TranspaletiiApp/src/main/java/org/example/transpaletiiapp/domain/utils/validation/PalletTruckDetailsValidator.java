package org.example.transpaletiiapp.domain.utils.validation;

import org.example.transpaletiiapp.domain.PalletTruckDetails;
import org.example.transpaletiiapp.domain.exceptions.ValidationException;

import java.time.LocalDate;

public class PalletTruckDetailsValidator implements Validator<PalletTruckDetails> {

    @Override
    public void validate(PalletTruckDetails entity) throws ValidationException {
        StringBuilder errors = new StringBuilder();

        if (entity == null) {
            throw new ValidationException("PalletTruckDetails cannot be null.");
        }

        if (entity.getTruck() == null) {
            errors.append("Details must be associated with a PalletTruck. ");
        }

        if (entity.getPurchaseDate() == null) {
            errors.append("Purchase date cannot be null. ");
        } else if (entity.getPurchaseDate().isAfter(LocalDate.now())) {
            errors.append("Purchase date cannot be in the future. ");
        }

        if (entity.getManufacturer() == null || entity.getManufacturer().trim().isEmpty()) {
            errors.append("Manufacturer cannot be null or empty. ");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}