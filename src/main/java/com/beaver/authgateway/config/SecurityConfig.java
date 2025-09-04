package com.beaver.authgateway.config;

import com.beaver.authgateway.auth.BootstrapSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    private final BootstrapSuccessHandler bootstrapSuccessHandler;

    SecurityConfig(BootstrapSuccessHandler bootstrapSuccessHandler) {
        this.bootstrapSuccessHandler = bootstrapSuccessHandler;
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository registrations
    ) {
        var resolver = new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/health", "/auth/dev/session").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(o -> o
                        .authorizationRequestResolver(resolver)
                        .authenticationSuccessHandler(bootstrapSuccessHandler)
                )
                .oauth2ResourceServer(o -> o.jwt(withDefaults()))
                .requestCache(rc -> rc.requestCache(new WebSessionServerRequestCache()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}