package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    @Value("${beaver.auth.kc.base}")
    private String kcBase;
    @Value("${beaver.auth.kc.env}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.auth-gateway.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.auth-gateway.client-secret}")
    private String clientSecret;

    Mono<String> adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        return web.build().post()
                .uri(kcBase + "/realms/{r}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (String) m.get("access_token"));
    }

    Mono<Map<String, Object>> getUser(String adminToken, String userId) {
        return web.build().get()
                .uri(kcBase + "/admin/realms/{r}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(adminToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    Mono<Void> putUser(String adminToken, String userId, Map<String, Object> fullUserJson) {
        return web.build().put()
                .uri(kcBase + "/admin/realms/{r}/users/{id}", realm, userId)
                .headers(h -> h.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fullUserJson)
                .retrieve()
                .bodyToMono(Void.class);
    }

    Mono<String> exchangeAccessToken(String subjectAccessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("subject_token", subjectAccessToken);
        form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

        return web.build().post()
                .uri(kcBase + "/realms/{r}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (String) m.get("access_token"));
    }
}
