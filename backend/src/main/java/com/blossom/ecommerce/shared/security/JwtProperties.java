package com.blossom.ecommerce.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secret     Base64-encoded HMAC-SHA key, at least 256 bits
 * @param expiration lifetime of an access token
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
