package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
    private final WebClient.Builder webClientBuilder;
    private final KeycloakClient kc;

    @Value("${beaver.internal-gateway.uri}")
    private String internalGatewayUri;

    private final RedirectServerAuthenticationSuccessHandler redirect = initRedirectHandler();

    private RedirectServerAuthenticationSuccessHandler initRedirectHandler() {
        RedirectServerAuthenticationSuccessHandler handler = new RedirectServerAuthenticationSuccessHandler();
        handler.setRequestCache(new WebSessionServerRequestCache());
        return handler;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oat)) {
            return redirect.onAuthenticationSuccess(exchange, authentication);
        }

        String registrationId = oat.getAuthorizedClientRegistrationId();

        return authorizedClientRepository
                .loadAuthorizedClient(registrationId, authentication, exchange.getExchange())
                .flatMap(client ->
                        bootstrapUser(client)
                                .flatMap(bootstrap -> {
                                    String accessToken =
                                            client.getAccessToken() != null ? client.getAccessToken().getTokenValue() : null;
                                    if (accessToken == null) return Mono.just(client);

                                    String sub;
                                    OAuth2User principal = oat.getPrincipal();
                                    if (principal instanceof OidcUser oidc) {
                                        sub = oidc.getSubject();
                                    } else {
                                        sub = principal.getAttribute("sub");
                                    }

                                    String userId = (String) bootstrap.get("userId");
                                    if (sub == null || userId == null) {
                                        log.warn("Missing sub or userId; skipping KC upsert & exchange");
                                        return Mono.just(client);
                                    }

                                    return upsertUserIdOnKeycloak(sub, userId)
                                            .then(kc.exchangeAccessToken(accessToken))
                                            .flatMap(exchangedAccessToken -> {
                                                OAuth2AuthorizedClient exchanged = new OAuth2AuthorizedClient(
                                                        client.getClientRegistration(),
                                                        oat.getName(),
                                                        new OAuth2AccessToken(
                                                                OAuth2AccessToken.TokenType.BEARER,
                                                                exchangedAccessToken,
                                                                Instant.now(),
                                                                Instant.now().plusSeconds(900)
                                                        ),
                                                        client.getRefreshToken()
                                                );
                                                return authorizedClientRepository
                                                        .saveAuthorizedClient(exchanged, authentication, exchange.getExchange())
                                                        .thenReturn(exchanged);
                                            });
                                })
                )
                .onErrorResume(ex -> {
                    log.warn("Bootstrap chain failed: {}", ex.toString());
                    return Mono.empty();
                })
                .then(redirect.onAuthenticationSuccess(exchange, authentication));
    }

    private Mono<Map<String,Object>> bootstrapUser(OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken() != null ? client.getAccessToken().getTokenValue() : null;
        if (accessToken == null) {
            log.warn("No access token available for bootstrap");
            return Mono.just(Map.of());
        }
        String url = internalGatewayUri + "/api/identity/users/bootstrap";
        return webClientBuilder.build().post()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                .doOnNext((Map<String,Object> body) -> log.info("Identity bootstrap OK: userId={}", body.get("userId")))
                .onErrorResume(ex -> {
                    log.warn("Bootstrap call failed: {}", ex.toString());
                    return Mono.just(Map.of());
                });
    }

    private Mono<Void> upsertUserIdOnKeycloak(String sub, String userId) {
        return kc.adminToken()
                .flatMap(admin -> kc.getUser(admin, sub)
                        .flatMap(existingObj -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> existing = existingObj;

                            @SuppressWarnings("unchecked")
                            Map<String, Object> attrs = (Map<String, Object>) existing.get("attributes");
                            if (attrs == null) {
                                attrs = new HashMap<>();
                                existing.put("attributes", attrs);
                            }
                            attrs.put("userId", new String[]{ userId });

                            return kc.putUser(admin, sub, existing);
                        })
                );
    }
}