package com.blossom.ecommerce.user.infrastructure.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[name=%s, email=%s, password=***]".formatted(name, email);
    }
}
