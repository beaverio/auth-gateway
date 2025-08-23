package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${INTERNAL_GATEWAY_URI}")
    private String internalGatewayUri;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return webFilterExchange.getChain().filter(webFilterExchange.getExchange());
        }

        String registrationId = token.getAuthorizedClientRegistrationId();

        return authorizedClientRepository
                .loadAuthorizedClient(registrationId, authentication, webFilterExchange.getExchange())
                .flatMap(this::bootstrapUser)
                .onErrorResume(ex -> {
                    log.warn("Bootstrap call failed: {}", ex.toString());
                    return Mono.empty();
                })
                .then(webFilterExchange.getChain().filter(webFilterExchange.getExchange()));
    }

    private Mono<Void> bootstrapUser(OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken() != null ? client.getAccessToken().getTokenValue() : null;
        if (accessToken == null) {
            log.warn("No access token available for bootstrap");
            return Mono.empty();
        }
        String url = internalGatewayUri + "/api/identity/users/bootstrap";
        WebClient webClient = webClientBuilder.build();
        return webClient.post()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(body -> {
                    Object userId = body.get("userId");
                    log.info("Identity bootstrap OK: userId={}", userId);
                })
                .then();
    }
}
