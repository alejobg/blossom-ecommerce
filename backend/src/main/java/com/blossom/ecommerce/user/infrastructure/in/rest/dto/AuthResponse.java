package com.blossom.ecommerce.user.infrastructure.in.rest.dto;

/**
 * @param expiresIn seconds until the access token expires
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
