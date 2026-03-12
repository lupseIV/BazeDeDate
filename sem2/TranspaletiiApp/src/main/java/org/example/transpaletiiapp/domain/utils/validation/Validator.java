package org.example.transpaletiiapp.domain.utils.validation;


import org.example.transpaletiiapp.domain.exceptions.ValidationException;

public interface Validator<T> {
    void validate(T entity) throws ValidationException;
}
