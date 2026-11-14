package com.blossom.ecommerce.user.domain;

/**
 * A signed credential issued after a successful login.
 */
public record AccessToken(String value, long expiresInSeconds) {

    @Override
    public String toString() {
        return "AccessToken[value=***, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
