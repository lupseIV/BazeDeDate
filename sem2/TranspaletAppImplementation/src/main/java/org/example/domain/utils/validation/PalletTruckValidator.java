package org.example.domain.utils.validation;

import org.example.domain.PalletTruck;
import org.example.domain.exceptions.ValidationException;

import java.util.Arrays;
import java.util.List;

public class PalletTruckValidator implements Validator<PalletTruck> {

    private static final List<String> VALID_STATUSES = Arrays.asList(
            "Available", "Rented", "In Maintenance", "Retired"
    );

    @Override
    public void validate(PalletTruck entity) throws ValidationException {
        StringBuilder errors = new StringBuilder();

        if (entity == null) {
            throw new ValidationException("PalletTruck cannot be null.");
        }

        if (entity.getSerialNumber() == null || entity.getSerialNumber().trim().isEmpty()) {
            errors.append("Serial number cannot be null or empty. ");
        }

        if (entity.getType() == null || entity.getType().trim().isEmpty()) {
            errors.append("Truck type cannot be null or empty. ");
        }

        if (entity.getModel() == null || entity.getModel().trim().isEmpty()) {
            errors.append("Model cannot be null or empty. ");
        }

        if (entity.getCapacityKg() == null || entity.getCapacityKg() <= 0) {
            errors.append("Capacity (kg) must be a positive number. ");
        }

        if (entity.getStatus() == null || !VALID_STATUSES.contains(entity.getStatus())) {
            errors.append("Status must be one of: ").append(String.join(", ", VALID_STATUSES)).append(". ");
        }

        if (entity.getWheel() == null) {
            errors.append("Pallet truck must have an associated Wheel. ");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}