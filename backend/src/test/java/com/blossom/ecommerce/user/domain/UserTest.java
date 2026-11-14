package com.blossom.ecommerce.user.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Email EMAIL = new Email("ana@mail.com");
    private static final String HASH = "$2a$10$hashed";

    @Test
    void registerCreatesNewCustomerWithTrimmedName() {
        Customer customer = Customer.register("  Ana Perez ", EMAIL, HASH);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getName()).isEqualTo("Ana Perez");
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getPasswordHash()).isEqualTo(HASH);
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void restoreBuildsTheSubtypeMatchingTheRole() {
        User customer = User.restore(1L, "Ana", EMAIL, HASH, Role.CUSTOMER);
        User admin = User.restore(2L, "Root", EMAIL, HASH, Role.ADMIN);

        assertThat(customer).isInstanceOf(Customer.class);
        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(admin).isInstanceOf(Admin.class);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void restoreRequiresIdAndRole() {
        assertThatThrownBy(() -> User.restore(null, "Ana", EMAIL, HASH, Role.CUSTOMER))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.restore(1L, "Ana", EMAIL, HASH, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsMissingName(String name) {
        assertThatThrownBy(() -> Customer.register(name, EMAIL, HASH))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("name is required");
    }

    @Test
    void rejectsTooLongName() {
        String name = "a".repeat(User.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> Customer.register(name, EMAIL, HASH))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsMissingEmail() {
        assertThatThrownBy(() -> Customer.register("Ana", null, HASH))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("email is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingPasswordHash(String hash) {
        assertThatThrownBy(() -> Customer.register("Ana", EMAIL, hash))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("password hash is required");
    }
}
