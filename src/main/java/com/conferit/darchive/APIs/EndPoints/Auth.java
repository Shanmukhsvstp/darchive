package com.conferit.darchive.APIs.EndPoints;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class Auth {

    @GetMapping("/me")
    public Map<String, Object> currentUser(
            @AuthenticationPrincipal OidcUser user) {

        return Map.of(
                "authenticated", true,
                "name", user.getFullName() != null
                        ? user.getFullName() : "",
                "email", user.getEmail() != null
                        ? user.getEmail() : "",
                "picture", user.getPicture() != null
                        ? user.getPicture() : ""
        );
    }
}