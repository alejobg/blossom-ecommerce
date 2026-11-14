package com.blossom.ecommerce.user.infrastructure.out.persistence;

import com.blossom.ecommerce.shared.persistence.BaseEntity;
import com.blossom.ecommerce.user.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Every account lives in one {@code users} table; the {@code role} column tells Hibernate
 * which subclass to instantiate. SINGLE_TABLE avoids joins, and the subtypes share all columns.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class UserEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    protected UserEntity() {
        // required by JPA
    }

    protected UserEntity(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public abstract Role getRole();

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
