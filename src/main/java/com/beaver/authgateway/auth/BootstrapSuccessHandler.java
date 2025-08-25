package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

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
        var handler = new RedirectServerAuthenticationSuccessHandler();
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
                        // 1) Ask identity-service to bootstrap user (204 on success)
                        bootstrapUser(client)
                                // 2) Then refresh tokens to pick up userId claim
                                .then(refreshAuthorizedClient(client, oat, exchange))
                                .onErrorResume(ex -> {
                                    log.warn("Bootstrap/refresh chain failed: {}", ex.toString());
                                    return Mono.just(client);
                                })
                )
                // Redirect back to the original endpoint
                .then(redirect.onAuthenticationSuccess(exchange, authentication));
    }

    /** POST to identity bootstrap (expects 204). */
    private Mono<Void> bootstrapUser(OAuth2AuthorizedClient client) {
        var accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.warn("No access token available for bootstrap");
            return Mono.empty();
        }
        String url = internalGatewayUri + "/api/identity/users/bootstrap";

        return webClientBuilder.build().post()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken.getTokenValue()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .doOnSuccess(x -> log.info("Identity bootstrap OK (204)"))
                .then();
    }

    /** Refresh access (and possibly refresh) token, then save back to the repo. */
    private Mono<OAuth2AuthorizedClient> refreshAuthorizedClient(
            OAuth2AuthorizedClient client,
            OAuth2AuthenticationToken oat,
            WebFilterExchange exchange) {

        OAuth2RefreshToken currentRt = client.getRefreshToken();
        if (currentRt == null) {
            log.warn("No refresh token available; cannot refresh to get userId claim");
            return Mono.just(client);
        }

        return kc.refreshAccessToken(currentRt.getTokenValue())
                .flatMap(tok -> {
                    Instant now = Instant.now();
                    var newAccess = getOAuth2AccessToken(client, tok, now);

                    OAuth2RefreshToken newRefresh = currentRt;
                    if (tok.refreshToken() != null && !tok.refreshToken().isBlank()) {
                        newRefresh = new OAuth2RefreshToken(tok.refreshToken(), now);
                    }

                    var exchanged = new OAuth2AuthorizedClient(
                            client.getClientRegistration(),
                            oat.getName(),
                            newAccess,
                            newRefresh
                    );

                    return authorizedClientRepository
                            .saveAuthorizedClient(exchanged, oat, exchange.getExchange())
                            .thenReturn(exchanged);
                });
    }

    private static OAuth2AccessToken getOAuth2AccessToken(OAuth2AuthorizedClient client, KeycloakClient.TokenResponse tok, Instant now) {
        Instant accessExpiresAt = tok.expiresIn() != null ? now.plusSeconds(tok.expiresIn()) : now.plusSeconds(900);
        Set<String> scopes = client.getAccessToken() != null ? client.getAccessToken().getScopes() : Set.of();

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tok.accessToken(),
                now,
                accessExpiresAt,
                scopes
        );
    }
}
