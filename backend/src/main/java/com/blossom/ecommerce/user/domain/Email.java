package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email value object. Normalized (trimmed, lower case) so lookups and the unique constraint
 * are case-insensitive: "Ana@Mail.com" and "ana@mail.com" are the same account.
 */
public record Email(String value) {

    static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("email is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("email is not valid");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
