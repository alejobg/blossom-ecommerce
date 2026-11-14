package com.blossom.ecommerce.user.domain.port.in;

public record RegisterCommand(String name, String email, String password) {

    /** Keeps the raw password out of logs. */
    @Override
    public String toString() {
        return "RegisterCommand[name=%s, email=%s, password=***]".formatted(name, email);
    }
}
