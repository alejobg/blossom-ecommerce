package com.blossom.ecommerce.user.application;

import com.blossom.ecommerce.shared.domain.InvalidCredentialsException;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import com.blossom.ecommerce.user.domain.Customer;
import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.EmailAlreadyRegisteredException;
import com.blossom.ecommerce.user.domain.PasswordPolicy;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.in.AuthResult;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;
import com.blossom.ecommerce.user.domain.port.in.UserUseCase;
import com.blossom.ecommerce.user.domain.port.out.PasswordHasher;
import com.blossom.ecommerce.user.domain.port.out.TokenIssuer;
import com.blossom.ecommerce.user.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements UserUseCase {

    private static final String RESOURCE = "User";

    private final UserRepositoryPort userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public UserService(UserRepositoryPort userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public User register(RegisterCommand command) {
        Email email = new Email(command.email());
        PasswordPolicy.validate(command.password());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        Customer customer = Customer.register(command.name(), email, passwordHasher.hash(command.password()));
        return userRepository.add(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByEmail(new Email(command.email()))
                .filter(candidate -> passwordHasher.matches(command.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new AuthResult(user, tokenIssuer.issue(user));
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
