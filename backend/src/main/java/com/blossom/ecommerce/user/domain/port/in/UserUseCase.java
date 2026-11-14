package com.blossom.ecommerce.user.domain.port.in;

import com.blossom.ecommerce.user.domain.User;

/**
 * Input port: account registration, authentication and lookup.
 */
public interface UserUseCase {

    /** Public sign-up: always creates a {@code Customer}. */
    User register(RegisterCommand command);

    AuthResult login(LoginCommand command);

    User getById(Long id);
}
