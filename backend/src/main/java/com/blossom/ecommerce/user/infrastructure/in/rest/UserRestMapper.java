package com.blossom.ecommerce.user.infrastructure.in.rest;

import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.in.AuthResult;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.AuthResponse;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.LoginRequest;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.RegisterRequest;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.UserResponse;

final class UserRestMapper {

    private static final String TOKEN_TYPE = "Bearer";

    private UserRestMapper() {
    }

    static RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(request.name(), request.email(), request.password());
    }

    static LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail().value(), user.getRole());
    }

    static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token().value(), TOKEN_TYPE, result.token().expiresInSeconds(),
                toResponse(result.user()));
    }
}
