package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.nio.charset.StandardCharsets;

/**
 * Rules for a raw password before it is hashed. The raw value never reaches the {@link User} model.
 */
public final class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    /** BCrypt only uses the first 72 bytes; longer input would be silently truncated. */
    static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new DomainValidationException("password must have at least " + MIN_LENGTH + " characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new DomainValidationException("password must have at most " + MAX_BYTES + " bytes");
        }
        if (rawPassword.chars().noneMatch(Character::isLetter) || rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new DomainValidationException("password must contain at least one letter and one digit");
        }
    }
}
