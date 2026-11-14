package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesToTrimmedLowerCase() {
        Email email = new Email("  Ana.Perez@Mail.COM ");

        assertThat(email.value()).isEqualTo("ana.perez@mail.com");
        assertThat(email).hasToString("ana.perez@mail.com");
    }

    @Test
    void emailsDifferingOnlyInCaseAreEqual() {
        assertThat(new Email("ANA@mail.com")).isEqualTo(new Email("ana@MAIL.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsMissingEmail(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "@mail.com", "ana@mail", "ana perez@mail.com", "ana@@mail.com"})
    void rejectsMalformedEmail(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("email is not valid");
    }

    @Test
    void rejectsTooLongEmail() {
        String value = "a".repeat(Email.MAX_LENGTH) + "@mail.com";

        assertThatThrownBy(() -> new Email(value)).isInstanceOf(DomainValidationException.class);
    }
}
