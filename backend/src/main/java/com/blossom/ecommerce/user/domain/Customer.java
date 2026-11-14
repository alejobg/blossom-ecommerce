package com.blossom.ecommerce.user.domain;

/**
 * Account created through public registration. Can browse the catalog and place orders.
 */
public final class Customer extends User {

    Customer(Long id, String name, Email email, String passwordHash) {
        super(id, name, email, passwordHash);
    }

    /** Creates a new, not yet persisted, customer. */
    public static Customer register(String name, Email email, String passwordHash) {
        return new Customer(null, name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}
