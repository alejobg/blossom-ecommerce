package com.blossom.ecommerce.user.application;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import com.blossom.ecommerce.shared.domain.InvalidCredentialsException;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.Customer;
import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.EmailAlreadyRegisteredException;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.in.AuthResult;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;
import com.blossom.ecommerce.user.domain.port.out.PasswordHasher;
import com.blossom.ecommerce.user.domain.port.out.TokenIssuer;
import com.blossom.ecommerce.user.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.blossom.ecommerce.user.UserFixtures.EMAIL;
import static com.blossom.ecommerce.user.UserFixtures.PASSWORD;
import static com.blossom.ecommerce.user.UserFixtures.PASSWORD_HASH;
import static com.blossom.ecommerce.user.UserFixtures.existingCustomer;
import static com.blossom.ecommerce.user.UserFixtures.validLoginCommand;
import static com.blossom.ecommerce.user.UserFixtures.validRegisterCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private UserService userService;

    @Test
    void registerStoresCustomerWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmail(new Email(EMAIL))).thenReturn(false);
        when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.add(any(User.class))).thenReturn(existingCustomer(1L));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        User created = userService.register(new RegisterCommand("Ana Perez", "  ANA@mail.com ", PASSWORD));

        verify(userRepository).add(captor.capture());
        User saved = captor.getValue();
        assertThat(saved).isInstanceOf(Customer.class);
        assertThat(saved.getEmail().value()).isEqualTo(EMAIL);
        assertThat(saved.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(created.getId()).isEqualTo(1L);
    }

    @Test
    void registerFailsWhenEmailIsTaken() {
        when(userRepository.existsByEmail(new Email(EMAIL))).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRegisterCommand()))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email 'ana@mail.com' is already registered");
        verify(userRepository, never()).add(any());
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void registerRejectsWeakPasswordBeforeTouchingTheRepository() {
        RegisterCommand weak = new RegisterCommand("Ana", EMAIL, "short");

        assertThatThrownBy(() -> userService.register(weak)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = existingCustomer(1L);
        AccessToken token = new AccessToken("jwt", 3600);
        when(userRepository.findByEmail(new Email(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(token);

        AuthResult result = userService.login(new LoginCommand(" Ana@Mail.com", PASSWORD));

        assertThat(result.user()).isSameAs(user);
        assertThat(result.token()).isEqualTo(token);
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail(new Email(EMAIL))).thenReturn(Optional.of(existingCustomer(1L)));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(validLoginCommand()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void loginFailsWithTheSameErrorForUnknownEmail() {
        when(userRepository.findByEmail(new Email(EMAIL))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(validLoginCommand()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void getByIdReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingCustomer(1L)));

        assertThat(userService.getById(1L).getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void getByIdFailsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id 99 was not found");
    }
}
