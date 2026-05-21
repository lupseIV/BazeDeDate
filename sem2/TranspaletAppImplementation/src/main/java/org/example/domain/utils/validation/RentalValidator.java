package org.example.domain.utils.validation;

import org.example.domain.Rental;
import org.example.domain.exceptions.ValidationException;

public class RentalValidator implements Validator<Rental> {
    @Override
    public void validate(Rental entity) {
        StringBuilder errors = new StringBuilder();
        if (entity.getTruck() == null) errors.append("Truck cannot be null.\n");
        if (entity.getStartDate() == null) errors.append("Start date cannot be null.\n");
        if (entity.getReturnStatus() == null || entity.getReturnStatus().trim().isEmpty()) {
            errors.append("Return status cannot be null or empty.\n");
        }
        if (entity.getEndDate() != null && entity.getStartDate() != null && entity.getEndDate().isBefore(entity.getStartDate())) {
            errors.append("End date cannot be before start date.\n");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString());
        }
    }
}