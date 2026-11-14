package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.util.Objects;

/**
 * Base type of every account. Sealed: an account is either a {@link Customer} or an {@link Admin},
 * so mappers can switch over the subtype exhaustively. Persisted with JPA SINGLE_TABLE inheritance.
 */
public abstract sealed class User permits Customer, Admin {

    static final int MAX_NAME_LENGTH = 100;

    private final Long id;
    private final String name;
    private final Email email;
    private final String passwordHash;

    protected User(Long id, String name, Email email, String passwordHash) {
        this.id = id;
        this.name = requireValidName(name);
        if (email == null) {
            throw new DomainValidationException("email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainValidationException("password hash is required");
        }
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** Rebuilds an existing account of the right subtype (used by the persistence adapter). */
    public static User restore(Long id, String name, Email email, String passwordHash, Role role) {
        Objects.requireNonNull(id, "id is required to restore a user");
        Objects.requireNonNull(role, "role is required to restore a user");
        return switch (role) {
            case CUSTOMER -> new Customer(id, name, email, passwordHash);
            case ADMIN -> new Admin(id, name, email, passwordHash);
        };
    }

    public abstract Role getRole();

    private static String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException("name must have at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
