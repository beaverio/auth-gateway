package com.beaver.authgateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> getAuthenticatedUser(
            @AuthenticationPrincipal OidcUser principal) {

        if (principal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated")));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", principal.getSubject());
        userInfo.put("username", principal.getPreferredUsername());
        userInfo.put("email", principal.getEmail());
        userInfo.put("name", principal.getFullName());
        userInfo.put("givenName", principal.getGivenName());
        userInfo.put("familyName", principal.getFamilyName());

        return Mono.just(ResponseEntity.ok(userInfo));
    }

    @GetMapping("/token")
    public Mono<ResponseEntity<Map<String, Object>>> getToken(
            @RegisteredOAuth2AuthorizedClient("auth-gateway") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OidcUser principal) {

        if (authorizedClient == null || principal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated")));
        }

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("access_token", authorizedClient.getAccessToken().getTokenValue());
        tokenInfo.put("token_type", "Bearer");
        tokenInfo.put("expires_at", authorizedClient.getAccessToken().getExpiresAt());
        tokenInfo.put("issued_at", authorizedClient.getAccessToken().getIssuedAt());
        tokenInfo.put("scopes", authorizedClient.getAccessToken().getScopes());

        if (principal.getIdToken() != null) {
            tokenInfo.put("id_token", principal.getIdToken().getTokenValue());
        }

        if (authorizedClient.getRefreshToken() != null) {
            tokenInfo.put("refresh_token", authorizedClient.getRefreshToken().getTokenValue());
            tokenInfo.put("refresh_token_expires_at", authorizedClient.getRefreshToken().getExpiresAt());
        }

        return Mono.just(ResponseEntity.ok(tokenInfo));
    }
}
