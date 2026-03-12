package org.example.domain.utils.validation;

import org.example.domain.exceptions.ValidationException;

public interface Validator<T> {
    void validate(T entity) throws ValidationException;
}
