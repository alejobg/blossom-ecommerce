package com.blossom.ecommerce.user.domain.port.out;

import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.User;

import java.util.Optional;

/**
 * Output port: what the user module needs from persistence, expressed in domain terms.
 */
public interface UserRepositoryPort {

    /** Persists a new account; fails with {@code EmailAlreadyRegisteredException} if the email is taken. */
    User add(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
