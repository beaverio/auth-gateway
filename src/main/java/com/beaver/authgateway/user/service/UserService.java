package com.beaver.authgateway.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final WebClient.Builder webClientBuilder;
    // TODO: Inject your session cache service here
    // private final SessionCacheService sessionCacheService;

    @Value("${beaver.internal-gateway.uri}")
    private String internalGatewayUri;

    public Mono<Void> deleteSelf(Jwt jwt) {
        log.debug("Calling downstream identity-service to delete user");

        return webClientBuilder.build()
            .delete()
            .uri(internalGatewayUri + "/api/identity/users/self")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(response -> {
                log.info("User deletion successful, invalidating session cache");
                // TODO: Invalidate session cache here
                // sessionCacheService.invalidateCurrentSession(jwt);
            })
            .doOnError(error -> log.error("Failed to delete user from identity-service: {}", error.getMessage()));
    }

    /** POST to identity bootstrap (expects 204). */
    public Mono<Void> bootstrapUser(OAuth2AuthorizedClient client) {
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
                .doOnSuccess(x -> log.info("identity-service user bootstrap: OK (204)"))
                .then();
    }
}
