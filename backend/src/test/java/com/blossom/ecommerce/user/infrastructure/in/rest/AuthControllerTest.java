package com.blossom.ecommerce.user.infrastructure.in.rest;

import com.blossom.ecommerce.shared.domain.InvalidCredentialsException;
import com.blossom.ecommerce.shared.security.SecurityConfig;
import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.EmailAlreadyRegisteredException;
import com.blossom.ecommerce.user.domain.port.in.AuthResult;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;
import com.blossom.ecommerce.user.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.blossom.ecommerce.user.UserFixtures.existingCustomer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice with the real security configuration: the auth endpoints must be reachable anonymously.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase userUseCase;

    @Test
    void registerReturns201WithTheCreatedUserAndNoPassword() throws Exception {
        when(userUseCase.register(any(RegisterCommand.class))).thenReturn(existingCustomer(1L));

        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Perez", "email": "ana@mail.com", "password": "secret123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/me"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userUseCase).register(new RegisterCommand("Ana Perez", "ana@mail.com", "secret123"));
    }

    @Test
    void registerReturnsFieldErrorsWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "", "email": "not-an-email", "password": "short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userUseCase);
    }

    @Test
    void registerReturns409WhenEmailIsTaken() throws Exception {
        when(userUseCase.register(any(RegisterCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException(new Email("ana@mail.com")));

        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Perez", "email": "ana@mail.com", "password": "secret123"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Email 'ana@mail.com' is already registered"));
    }

    @Test
    void loginReturnsBearerTokenAndUser() throws Exception {
        when(userUseCase.login(new LoginCommand("ana@mail.com", "secret123")))
                .thenReturn(new AuthResult(existingCustomer(1L), new AccessToken("jwt-value", 3600)));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "ana@mail.com", "password": "secret123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void loginReturns401WithInvalidCredentials() throws Exception {
        when(userUseCase.login(any(LoginCommand.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "ana@mail.com", "password": "wrong-pass1"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginRequiresEmailAndPassword() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
