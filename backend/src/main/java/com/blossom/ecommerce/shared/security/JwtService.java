package com.blossom.ecommerce.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Signs and verifies HMAC-SHA access tokens (HS256/384/512, chosen by jjwt from the key length).
 * The token is self-contained (user id, email, role), so authenticating a request needs no database lookup.
 */
public class JwtService {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        // hmacShaKeyFor rejects keys shorter than 256 bits, so a weak secret fails at startup
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    public String issue(Long userId, String email, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Returns the identity in the token, or empty if it is malformed, tampered with or expired. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(Long.valueOf(claims.getSubject()),
                    claims.get(EMAIL_CLAIM, String.class), claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Duration expiration() {
        return expiration;
    }
}
