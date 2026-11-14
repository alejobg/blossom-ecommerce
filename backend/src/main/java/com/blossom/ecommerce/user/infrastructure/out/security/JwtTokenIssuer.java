package com.blossom.ecommerce.user.infrastructure.out.security;

import com.blossom.ecommerce.shared.security.JwtService;
import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.out.TokenIssuer;
import org.springframework.stereotype.Component;

@Component
class JwtTokenIssuer implements TokenIssuer {

    private final JwtService jwtService;

    JwtTokenIssuer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public AccessToken issue(User user) {
        String token = jwtService.issue(user.getId(), user.getEmail().value(), user.getRole().name());
        return new AccessToken(token, jwtService.expiration().toSeconds());
    }
}
