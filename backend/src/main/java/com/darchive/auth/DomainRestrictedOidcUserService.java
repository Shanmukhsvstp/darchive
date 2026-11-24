package com.darchive.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Only lets @bmsce.ac.in Google accounts in, mirrors them into users, promotes configured admins. */
@Service
public class DomainRestrictedOidcUserService extends OidcUserService {

    private final UserRepository users;
    private final String allowedDomain;
    private final Set<String> adminEmails;

    public DomainRestrictedOidcUserService(UserRepository users,
                                           @Value("${app.allowed-email-domain}") String allowedDomain,
                                           @Value("${app.admin-emails:}") String adminEmails) {
        this.users = users;
        this.allowedDomain = allowedDomain.toLowerCase();
        this.adminEmails = Arrays.stream(adminEmails.split(","))
            .map(String::trim).map(String::toLowerCase).filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(request);

        String email = user.getEmail();
        boolean verified = Boolean.TRUE.equals(user.getEmailVerified());
        if (email == null || !verified || !email.toLowerCase().endsWith("@" + allowedDomain)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_domain"), "Only @" + allowedDomain + " accounts are allowed");
        }

        users.upsert(user.getSubject(), email, user.getFullName(), user.getPicture());
        if (adminEmails.contains(email.toLowerCase())) users.makeAdmin(email);
        return user;
    }
}
