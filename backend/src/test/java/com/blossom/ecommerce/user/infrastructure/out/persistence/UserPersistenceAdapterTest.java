package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.blossom.ecommerce.user.domain.Admin;
import com.blossom.ecommerce.user.domain.Customer;
import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.EmailAlreadyRegisteredException;
import com.blossom.ecommerce.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static com.blossom.ecommerce.user.UserFixtures.existingCustomer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence adapter against a real PostgreSQL: SINGLE_TABLE mapping, discriminator and unique email.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserPersistenceAdapter.class})
@ActiveProfiles("test")
class UserPersistenceAdapterTest {

    private static final Email ANA = new Email("ana@mail.com");

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void addAssignsIdAndRoundTripsCustomer() {
        User saved = adapter.add(Customer.register("Ana Perez", ANA, "$2a$10$hash"));

        User found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(found).isInstanceOf(Customer.class);
        assertThat(found.getName()).isEqualTo("Ana Perez");
        assertThat(found.getEmail()).isEqualTo(ANA);
        assertThat(found.getPasswordHash()).isEqualTo("$2a$10$hash");
    }

    @Test
    void roleIsStoredAsDiscriminatorColumn() {
        User saved = adapter.add(Customer.register("Ana Perez", ANA, "$2a$10$hash"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, saved.getId());
        assertThat(role).isEqualTo("CUSTOMER");
    }

    @Test
    void initialAdminFromMigrationIsLoadedAsAdmin() {
        User admin = adapter.findByEmail(new Email("admin@blossom.com")).orElseThrow();

        assertThat(admin).isInstanceOf(Admin.class);
    }

    @Test
    void findByEmailAndExistsByEmail() {
        adapter.add(Customer.register("Ana Perez", ANA, "$2a$10$hash"));

        assertThat(adapter.findByEmail(ANA)).isPresent();
        assertThat(adapter.existsByEmail(ANA)).isTrue();
        assertThat(adapter.findByEmail(new Email("nobody@mail.com"))).isEmpty();
        assertThat(adapter.existsByEmail(new Email("nobody@mail.com"))).isFalse();
    }

    @Test
    void duplicateEmailIsTranslatedToDomainException() {
        adapter.add(Customer.register("Ana Perez", ANA, "$2a$10$hash"));

        assertThatThrownBy(() -> adapter.add(Customer.register("Other Ana", ANA, "$2a$10$hash")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void addRejectsUsersThatAlreadyHaveAnId() {
        assertThatThrownBy(() -> adapter.add(existingCustomer(1L))).isInstanceOf(IllegalArgumentException.class);
    }
}
