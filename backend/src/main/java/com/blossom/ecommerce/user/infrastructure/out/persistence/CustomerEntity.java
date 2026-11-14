package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.user.domain.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CUSTOMER")
public class CustomerEntity extends UserEntity {

    protected CustomerEntity() {
        // required by JPA
    }

    CustomerEntity(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}
