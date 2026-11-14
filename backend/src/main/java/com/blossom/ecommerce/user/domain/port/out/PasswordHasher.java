package com.blossom.ecommerce.user.domain.port.out;

/**
 * One-way password hashing, so the domain does not depend on a crypto library.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
