package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.EmailAlreadyRegisteredException;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.out.UserRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    UserPersistenceAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User add(User user) {
        if (user.getId() != null) {
            throw new IllegalArgumentException("Only new users can be added");
        }
        try {
            return UserPersistenceMapper.toDomain(jpaRepository.save(UserPersistenceMapper.toNewEntity(user)));
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent sign-ups can both pass the service's existsByEmail check;
            // the unique constraint is the real guarantee. Email is the only unique column.
            throw new EmailAlreadyRegisteredException(user.getEmail());
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }
}
