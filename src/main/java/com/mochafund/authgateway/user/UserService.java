package com.mochafund.authgateway.user;

import com.mochafund.authgateway.session.SessionsService;
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
    private final SessionsService sessions;

    @Value("${mochafund.internal-gateway.uri}")
    private String internalGatewayUri;

    public Mono<Void> deleteSelf(Jwt jwt) {
        String principal = jwt.getClaimAsString("preferred_username");
        log.info("[self-deletion-request] Request received for {}", principal);

        return webClientBuilder.build()
                .delete()
                .uri(internalGatewayUri + "/api/identity/users/self")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .then(sessions.deleteAllByPrincipal(principal)
                    .doOnNext(cnt -> log.info("[self-deletion-request] Sessions deleted for {}", principal))
                    .then()
                )
                .doOnSuccess(x -> log.info("[self-deletion-request] Request completed for {}", principal));
    }

    public Mono<Void> bootstrapUser(OAuth2AuthorizedClient client) {
        var accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.warn("[bootstrap-user-request] No access token available; cannot bootstrap user");
            return Mono.empty();
        }
        String url = internalGatewayUri + "/api/identity/users/bootstrap";

        return webClientBuilder.build().post()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken.getTokenValue()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .doOnSuccess(x -> log.info(
                        "[bootstrap-user-request] Bootstrap user request completed for {}", client.getPrincipalName()))
                .then();
    }
}
