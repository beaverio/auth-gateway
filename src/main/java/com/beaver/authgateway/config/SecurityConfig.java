package com.beaver.authgateway.config;

import com.beaver.authgateway.auth.BootstrapSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
class SecurityConfig {

    private final BootstrapSuccessHandler bootstrapSuccessHandler;

    SecurityConfig(BootstrapSuccessHandler bootstrapSuccessHandler) {
        this.bootstrapSuccessHandler = bootstrapSuccessHandler;
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/health", "/auth/dev/session").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(o -> o.authenticationSuccessHandler(bootstrapSuccessHandler))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}