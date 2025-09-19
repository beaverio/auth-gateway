package com.mochafund.authgateway.user.consumer;

import com.mochafund.authgateway.session.SessionsService;
import com.mochafund.authgateway.user.events.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserEventConsumer {

    private final SessionsService sessions;

    @KafkaListener(topics = "user.deleted", groupId = "auth-gateway")
    public void handleUserDeleted(UserEvent event) {
        String email = event.getData().email();
        log.info("Processing user.deleted - User: {}", email);

        sessions.listByPrincipal(email)
            .doOnNext(sessionList -> log.info("Found {} sessions for user {}", sessionList.size(), email))
            .then(sessions.deleteAllByPrincipal(email))
            .doOnNext(count -> log.info("Deleted {} sessions for user: {}", count, email))
            .subscribe();
    }

    @KafkaListener(topics = "user.updated", groupId = "auth-gateway")
    public void handleUserUpdated(UserEvent event) {
        String email = event.getData().email();
        boolean invalidate = event.getData().invalidate();

        log.info("Processing user.updated - User: {}, invalidate: {}", email, invalidate);

        if (invalidate) {
            sessions.deleteAllByPrincipal(email)
                .doOnNext(count -> log.info("Invalidated {} sessions for user: {}", count, email))
                .subscribe();
        } else {
            // TODO: Refresh all sessions for this user!
            log.warn("User updated refreshing sessions - User: {}", email);
        }
    }
}
