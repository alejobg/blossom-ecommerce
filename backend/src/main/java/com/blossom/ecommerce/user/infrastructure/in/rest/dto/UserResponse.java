package com.blossom.ecommerce.user.infrastructure.in.rest.dto;

import com.blossom.ecommerce.user.domain.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}
