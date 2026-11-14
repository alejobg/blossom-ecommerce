package com.blossom.ecommerce.user.domain;

/**
 * Back-office account that manages the catalog. It cannot be created through the public API:
 * the initial admin comes from a database migration.
 */
public final class Admin extends User {

    Admin(Long id, String name, Email email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
