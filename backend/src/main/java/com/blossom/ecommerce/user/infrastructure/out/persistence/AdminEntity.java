package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.user.domain.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class AdminEntity extends UserEntity {

    protected AdminEntity() {
        // required by JPA
    }

    AdminEntity(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
