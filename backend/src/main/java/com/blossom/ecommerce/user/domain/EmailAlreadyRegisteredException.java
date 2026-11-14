package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(Email email) {
        super("Email '%s' is already registered".formatted(email));
    }
}
