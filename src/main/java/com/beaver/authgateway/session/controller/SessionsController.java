package com.beaver.authgateway.session.controller;

import com.beaver.authgateway.session.service.SessionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/auth/sessions")
@RequiredArgsConstructor
public class SessionsController {

    private final SessionsService sessions;

    @GetMapping()
    public Mono<List<SessionsService.SessionSummary>> getSessions(Authentication auth) {
        return sessions.listByPrincipal(auth.getName());
    }
}

