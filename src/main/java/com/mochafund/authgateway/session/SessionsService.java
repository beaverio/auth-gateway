package com.mochafund.authgateway.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.ReactiveFindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionsService {

    private final ReactiveSessionRepository<? extends Session> sessionRepo;

    private ReactiveFindByIndexNameSessionRepository<? extends Session> indexed() {
        if (sessionRepo instanceof ReactiveFindByIndexNameSessionRepository<?> idx) {
            return idx;
        }
        throw new IllegalStateException(
                "Reactive Redis session repo is NOT indexed. Set `spring.session.redis.repository-type=indexed`.");
    }

    public Mono<List<SessionSummary>> listByPrincipal(String principal) {
        return indexed().findByPrincipalName(principal)
                .map(Map::values)
                .map(values -> values.stream()
                        .map(SessionSummary::from)
                        .sorted(Comparator.comparing(SessionSummary::lastAccessed).reversed())
                        .toList());
    }

    public Mono<Long> deleteAllByPrincipal(String principal) {
        return indexed().findByPrincipalName(principal)
                .flatMapMany(map -> {
                    var sessionIds = map.keySet();
                    return Flux.fromIterable(sessionIds)
                            .flatMap(sessionId ->
                                sessionRepo.deleteById(sessionId)
                                    .doOnSuccess(v -> log.debug("Deleted session: {}", sessionId))
                                    .thenReturn(1L)
                            );
                })
                .reduce(0L, Long::sum);
    }

    public record SessionSummary(String id, Instant created, Instant lastAccessed, Instant expiresAt) {
        static SessionSummary from(Session s) {
            var expiry = s.getLastAccessedTime().plus(s.getMaxInactiveInterval());
            return new SessionSummary(s.getId(), s.getCreationTime(), s.getLastAccessedTime(), expiry);
        }
    }
}