package com.blossom.ecommerce.user;

import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.LoginRequest;
import com.blossom.ecommerce.user.infrastructure.in.rest.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Records print every component by default; these must never write a password or token to a log.
 */
class SecretMaskingTest {

    private static final String SECRET = "secret123";

    @Test
    void credentialsAreMaskedInToString() {
        assertThat(new RegisterCommand("Ana", "ana@mail.com", SECRET).toString())
                .contains("ana@mail.com").doesNotContain(SECRET);
        assertThat(new LoginCommand("ana@mail.com", SECRET).toString()).doesNotContain(SECRET);
        assertThat(new RegisterRequest("Ana", "ana@mail.com", SECRET).toString()).doesNotContain(SECRET);
        assertThat(new LoginRequest("ana@mail.com", SECRET).toString()).doesNotContain(SECRET);
    }

    @Test
    void accessTokenIsMaskedInToString() {
        assertThat(new AccessToken("eyJhbGciOi.payload.signature", 3600).toString())
                .doesNotContain("eyJhbGciOi")
                .contains("3600");
    }
}
