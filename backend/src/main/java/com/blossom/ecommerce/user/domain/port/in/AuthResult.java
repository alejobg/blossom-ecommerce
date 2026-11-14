package com.blossom.ecommerce.user.domain.port.in;

import com.blossom.ecommerce.user.domain.AccessToken;
import com.blossom.ecommerce.user.domain.User;

public record AuthResult(User user, AccessToken token) {
}
