package com.beaver.authgateway.config;

import com.beaver.authgateway.auth.SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    private final SuccessHandler successHandler;

    SecurityConfig(SuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository registrations
    ) {
        var resolver = new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        var oidcLogout = new OidcClientInitiatedServerLogoutSuccessHandler(registrations);
        oidcLogout.setPostLogoutRedirectUri("{baseUrl}/auth/logged-out");

        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/health", "/auth/logged-out").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(o -> o
                        .authorizationRequestResolver(resolver)
                        .authenticationSuccessHandler(successHandler)
                )
                .oauth2ResourceServer(o -> o.jwt(withDefaults()))
                .logout(l -> l
                        .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/auth/logout"))
                        .logoutHandler((exchange, auth) ->
                                exchange.getExchange().getSession().flatMap(org.springframework.web.server.WebSession::invalidate))
                        .logoutSuccessHandler(oidcLogout)
                )
                .requestCache(rc -> rc.requestCache(new WebSessionServerRequestCache()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}