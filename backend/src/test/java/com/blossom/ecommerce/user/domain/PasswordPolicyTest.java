package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"secret12", "Pass word 1", "ñandú2024"})
    void acceptsPasswordsWithLettersAndDigits(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "abc123")
    void rejectsTooShortPassword(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("password must have at least 8 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"onlyletters", "12345678"})
    void requiresLettersAndDigits(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("password must contain at least one letter and one digit");
    }

    @Test
    void acceptsExactlyMaxBytes() {
        String password = "a1" + "b".repeat(PasswordPolicy.MAX_BYTES - 2);

        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordsBcryptWouldTruncate() {
        // 40 characters but 80 bytes in UTF-8
        String password = "1" + "ñ".repeat(40);

        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("password must have at most 72 bytes");
    }
}
