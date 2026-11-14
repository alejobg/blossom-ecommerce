package com.blossom.ecommerce.shared.security;

import java.security.Principal;

/**
 * Identity carried by a valid access token. Controllers receive it with {@code @AuthenticationPrincipal}.
 * The role is a plain string so this shared type does not depend on the user module.
 */
public record AuthenticatedUser(Long id, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
