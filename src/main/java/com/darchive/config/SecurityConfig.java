package com.darchive.config;

import com.darchive.auth.DomainRestrictedOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    DomainRestrictedOidcUserService oidcUserService,
                                    @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // Let the frontend show uploaded PDFs in an <iframe> preview (replaces X-Frame-Options: DENY).
            .headers(h -> h
                .frameOptions(f -> f.disable())
                .contentSecurityPolicy(c -> c.policyDirectives("frame-ancestors 'self' " + frontendUrl)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/csrf", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(o -> o
                .userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
                .defaultSuccessUrl(frontendUrl, true)
                .failureUrl(frontendUrl + "/auth?error=not_allowed"))
            .logout(l -> l
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value())))
            // API calls get a plain 401 (frontend redirects to /auth) instead of an HTML redirect to Google.
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url}") String frontendUrl) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(frontendUrl));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", c);
        return src;
    }
}
