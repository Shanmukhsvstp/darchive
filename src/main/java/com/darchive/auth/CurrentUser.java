package com.darchive.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the logged-in Google principal to our users row. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public UserRepository.User require(OidcUser principal) {
        return users.findByGoogleSub(principal.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
