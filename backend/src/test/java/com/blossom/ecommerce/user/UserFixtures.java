package com.blossom.ecommerce.user;

import com.blossom.ecommerce.user.domain.Email;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.domain.port.in.LoginCommand;
import com.blossom.ecommerce.user.domain.port.in.RegisterCommand;

public final class UserFixtures {

    public static final String EMAIL = "ana@mail.com";
    public static final String PASSWORD = "secret123";
    public static final String PASSWORD_HASH = "$2a$10$hashed";

    private UserFixtures() {
    }

    public static User existingCustomer(Long id) {
        return User.restore(id, "Ana Perez", new Email(EMAIL), PASSWORD_HASH, Role.CUSTOMER);
    }

    public static User existingAdmin(Long id) {
        return User.restore(id, "Admin", new Email("admin@mail.com"), PASSWORD_HASH, Role.ADMIN);
    }

    public static RegisterCommand validRegisterCommand() {
        return new RegisterCommand("Ana Perez", EMAIL, PASSWORD);
    }

    public static LoginCommand validLoginCommand() {
        return new LoginCommand(EMAIL, PASSWORD);
    }
}
