package com.mochafund.authgateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final WebClient.Builder webClientBuilder;

    @Value("${mochafund.internal-gateway.uri}")
    private String internalGatewayUri;

    public Mono<Void> bootstrapUser(OAuth2AuthorizedClient client) {
        var accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.warn("No access token available; cannot bootstrap user");
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
                        "Bootstrap user request completed for {}", client.getPrincipalName()))
                .then();
    }
}
