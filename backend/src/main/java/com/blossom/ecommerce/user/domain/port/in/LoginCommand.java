package com.blossom.ecommerce.user.domain.port.in;

public record LoginCommand(String email, String password) {

    /** Keeps the raw password out of logs. */
    @Override
    public String toString() {
        return "LoginCommand[email=%s, password=***]".formatted(email);
    }
}
