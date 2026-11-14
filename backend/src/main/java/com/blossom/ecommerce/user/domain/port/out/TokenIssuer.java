package com.blossom.ecommerce.user.domain.port.out;

import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.User;

/**
 * Issues the credential a client sends on later requests (a JWT, in this implementation).
 */
public interface TokenIssuer {

    AccessToken issue(User user);
}
