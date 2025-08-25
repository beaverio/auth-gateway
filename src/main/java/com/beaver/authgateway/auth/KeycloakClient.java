package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
class KeycloakClient {
    private final WebClient.Builder web;

    @Value("${beaver.auth.kc.base}") private String kcBase;
    @Value("${beaver.auth.kc.env}") private String realm;

    @Value("${spring.security.oauth2.client.registration.auth-gateway.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.auth-gateway.client-secret}")
    private String clientSecret;

    /** Refresh the user's tokens (regular OIDC refresh), returning new access and (possibly rotated) refresh tokens. */
    Mono<TokenResponse> refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        return web.build().post()
                .uri(kcBase + "/realms/{r}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> new TokenResponse(
                        (String) m.get("access_token"),
                        (String) m.getOrDefault("refresh_token", refreshToken),
                        toLong(m.get("expires_in")),
                        toLong(m.get("refresh_expires_in"))
                ));
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    /** Minimal response holder for refresh results. */
    record TokenResponse(String accessToken, String refreshToken, Long expiresIn, Long refreshExpiresIn) {}
}
