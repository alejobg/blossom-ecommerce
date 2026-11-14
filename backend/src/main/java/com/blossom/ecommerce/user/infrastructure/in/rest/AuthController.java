package com.blossom.ecommerce.user.infrastructure.in.rest;

import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.in.UserUseCase;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.AuthResponse;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.LoginRequest;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.RegisterRequest;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Customer sign-up and login")
@SecurityRequirements // public endpoints: no token in Swagger UI
public class AuthController {

    private final UserUseCase userUseCase;

    public AuthController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a customer account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = userUseCase.register(UserRestMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/me")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(UserRestMapper.toResponse(created));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for an access token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return UserRestMapper.toResponse(userUseCase.login(UserRestMapper.toCommand(request)));
    }
}
