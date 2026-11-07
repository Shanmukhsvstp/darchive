package com.darchive.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final CurrentUser currentUser;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public AuthController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    /** Shape expected by the frontend: { headerName, token }. */
    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/me")
    public UserRepository.User me(@AuthenticationPrincipal OidcUser principal) {
        return currentUser.require(principal);
    }

    @GetMapping("/")
    public String hi() {

        System.out.println("Google Client ID: " + clientId);

        // Never print the actual client secret
        System.out.println("Google Client Secret configured: "
                + (clientSecret != null && !clientSecret.isBlank()));

        return clientId;
    }
}
