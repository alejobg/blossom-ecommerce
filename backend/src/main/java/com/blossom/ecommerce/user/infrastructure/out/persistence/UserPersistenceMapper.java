package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.user.domain.Admin;
import com.blossom.ecommerce.user.domain.Customer;
import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.User;

final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    static User toDomain(UserEntity entity) {
        return User.restore(entity.getId(), entity.getName(), new Email(entity.getEmail()),
                entity.getPasswordHash(), entity.getRole());
    }

    /** Exhaustive over the sealed hierarchy: a new subtype will not compile until it is mapped here. */
    static UserEntity toNewEntity(User user) {
        String email = user.getEmail().value();
        return switch (user) {
            case Customer customer -> new CustomerEntity(customer.getName(), email, customer.getPasswordHash());
            case Admin admin -> new AdminEntity(admin.getName(), email, admin.getPasswordHash());
        };
    }
}
