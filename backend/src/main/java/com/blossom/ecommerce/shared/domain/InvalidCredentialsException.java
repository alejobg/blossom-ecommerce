package com.blossom.ecommerce.shared.domain;

/**
 * Login failed. The message is the same for an unknown email and for a wrong password,
 * so the API does not reveal which accounts exist.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
