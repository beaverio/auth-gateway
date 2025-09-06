package com.beaver.authgateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    @GetMapping(value = "/logged-out", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> done() {
        return Mono.just(ResponseEntity.ok("Logged out"));
    }
}
