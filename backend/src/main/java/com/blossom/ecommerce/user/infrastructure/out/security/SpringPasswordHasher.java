package com.blossom.ecommerce.user.infrastructure.out.security;

import com.blossom.ecommerce.user.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Delegates to Spring Security's {@link PasswordEncoder} (BCrypt).
 */
@Component
class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    SpringPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
