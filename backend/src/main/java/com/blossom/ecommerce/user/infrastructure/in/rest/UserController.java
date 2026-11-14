package com.blossom.ecommerce.user.infrastructure.in.rest;

import com.blossom.ecommerce.shared.security.AuthenticatedUser;
import com.blossom.ecommerce.user.domain.port.in.UserUseCase;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user's account")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserRestMapper.toResponse(userUseCase.getById(principal.id()));
    }
}
