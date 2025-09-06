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
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @DeleteMapping("/self")
    public Mono<ResponseEntity<Object>> deleteUserSelf(@AuthenticationPrincipal Jwt jwt, WebSession webSession) {
        return userService.deleteSelf(jwt)
            .then(webSession.invalidate())
            .thenReturn(ResponseEntity.noContent().build());
    }
}
