package com.beaver.authgateway.user.controller;

import com.beaver.authgateway.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/users")
public class UserController {

    private final UserService userService;

    @DeleteMapping("/self")
    public Mono<ResponseEntity<Object>> deleteUserSelf(@AuthenticationPrincipal Jwt jwt) {
        log.info("Processing user self-deletion request");

        return userService.deleteSelf(jwt)
            .then(Mono.fromCallable(() -> ResponseEntity.noContent().build()))
            .doOnSuccess(response -> log.info("User self-deletion completed successfully"))
            .doOnError(error -> log.error("User self-deletion failed: {}", error.getMessage()));
    }
}
