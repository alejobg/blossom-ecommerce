package com.blossom.ecommerce.shared.domain;

/**
 * Raised when an operation would break a domain invariant (negative price, empty name, ...).
 */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
