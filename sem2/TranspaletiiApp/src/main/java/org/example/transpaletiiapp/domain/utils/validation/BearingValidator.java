package org.example.transpaletiiapp.domain.utils.validation;

import org.example.transpaletiiapp.domain.Bearing;
import org.example.transpaletiiapp.domain.exceptions.ValidationException;

public class BearingValidator implements Validator<Bearing> {

    @Override
    public void validate(Bearing entity) throws ValidationException {
        StringBuilder errors = new StringBuilder();

        if (entity == null) {
            throw new ValidationException("Bearing cannot be null.");
        }

        if (entity.getDiameter() == null || entity.getDiameter() <= 0) {
            errors.append("Bearing diameter must be a positive number. ");
        }

        if (entity.getMid() != null && entity.getMid() <= 0) {
            errors.append("Bearing mid must be a positive number. ");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors.toString().trim());
        }
    }
}