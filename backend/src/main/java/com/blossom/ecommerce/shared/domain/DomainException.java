package com.blossom.ecommerce.shared.domain;

/**
 * Base type for every error raised by the domain or application layers.
 * The web layer maps subclasses to HTTP status codes; the domain never knows about HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
