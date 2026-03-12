package org.example.domain.utils.validation;

import org.example.domain.Wheel;
import org.example.domain.exceptions.ValidationException;

public class WheelValidator implements Validator<Wheel> {

    @Override
    public void validate(Wheel entity) throws ValidationException {
        StringBuilder errors = new StringBuilder();

        if (entity == null) {
            throw new ValidationException("Wheel cannot be null.");
        }

        if (entity.getMaterial() == null) {
            errors.append("Wheel must be associated with a WheelMaterial. ");
        }

        if (entity.getBearing() == null) {
            errors.append("Wheel must be associated with a Bearing. ");
        }

        if (entity.getMaxWeight() == null || entity.getMaxWeight() <= 0) {
            errors.append("Wheel max weight must be a positive number. ");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}