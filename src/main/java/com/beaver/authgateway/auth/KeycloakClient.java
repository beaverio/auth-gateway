package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KeycloakClient {
    private final WebClient.Builder web;

    @Value("${keycloak.admin.base-url}") String kcBase;
    @Value("${keycloak.admin.realm}")  String realm;
    @Value("${keycloak.admin.client-id}") String clientId;
    @Value("${keycloak.admin.client-secret}") String clientSecret;

    Mono<TokenResponse> refreshAccessToken(String refreshToken) {
        return web.build().post()
                .uri(kcBase + "/realms/{r}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "refresh_token")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(AccessTokenResponse.class)
                .map(atr -> new TokenResponse(
                        atr.getToken(),
                        atr.getRefreshToken() == null || atr.getRefreshToken().isBlank()
                                ? refreshToken : atr.getRefreshToken(),
                        atr.getExpiresIn(),
                        atr.getRefreshExpiresIn()
                ));
    }

    record TokenResponse(String accessToken, String refreshToken, Long expiresIn, Long refreshExpiresIn) {}
}
