package com.blossom.ecommerce.shared.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = base64Key('k');
    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final JwtService jwtService = serviceAt(NOW, SECRET);

    @Test
    void issuedTokenRoundTripsTheIdentity() {
        String token = jwtService.issue(7L, "ana@mail.com", "CUSTOMER");

        assertThat(jwtService.parse(token))
                .contains(new AuthenticatedUser(7L, "ana@mail.com", "CUSTOMER"));
    }

    @Test
    void tokenIsValidUntilItExpires() {
        String token = jwtService.issue(7L, "ana@mail.com", "CUSTOMER");

        assertThat(serviceAt(NOW.plus(EXPIRATION).minusSeconds(1), SECRET).parse(token)).isPresent();
        assertThat(serviceAt(NOW.plus(EXPIRATION).plusSeconds(1), SECRET).parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = serviceAt(NOW, base64Key('x')).issue(7L, "ana@mail.com", "ADMIN");

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.issue(7L, "ana@mail.com", "CUSTOMER");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"7\",\"role\":\"ADMIN\"}".getBytes());

        assertThat(jwtService.parse(parts[0] + "." + forgedPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }

    @Test
    void secretShorterThan256BitsFailsAtStartup() {
        String weak = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new JwtService(new JwtProperties(weak, EXPIRATION)))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    void exposesConfiguredExpiration() {
        assertThat(jwtService.expiration()).isEqualTo(EXPIRATION);
    }

    private static JwtService serviceAt(Instant instant, String secret) {
        return new JwtService(new JwtProperties(secret, EXPIRATION), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String base64Key(char filler) {
        return Base64.getEncoder().encodeToString(String.valueOf(filler).repeat(32).getBytes());
    }
}
