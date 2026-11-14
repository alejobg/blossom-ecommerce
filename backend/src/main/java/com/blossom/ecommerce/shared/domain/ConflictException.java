package com.blossom.ecommerce.shared.domain;

/**
 * Raised when an operation clashes with the current state of a resource (duplicate unique value, ...).
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
