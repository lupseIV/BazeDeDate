package org.example.domain.utils.validation;

import org.example.domain.WheelMaterial;
import org.example.domain.exceptions.ValidationException;

public class WheelMaterialValidator implements Validator<WheelMaterial> {

    @Override
    public void validate(WheelMaterial entity) throws ValidationException {
        StringBuilder errors = new StringBuilder();

        if (entity == null) {
            throw new ValidationException("WheelMaterial cannot be null.");
        }

        if (entity.getType() == null || entity.getType().trim().isEmpty()) {
            errors.append("Material type cannot be null or empty. ");
        }

        if (entity.getMaxWeight() == null || entity.getMaxWeight() <= 0) {
            errors.append("Max weight must be a positive number. ");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}