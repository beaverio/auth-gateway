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
        String oldEmail = event.getData().oldEmail();
        boolean invalidate = event.getData().invalidate();

        log.info("Processing user.updated - User: {} (old: {}), invalidate: {}", email, oldEmail, invalidate);

        if (invalidate) {
            String principalEmail = oldEmail != null ? oldEmail : email;

            sessions.listByPrincipal(principalEmail)
                    .doOnNext(sessionList -> log.info("Found {} sessions for user {} (principal: {})", sessionList.size(), email, principalEmail))
                    .then(sessions.deleteAllByPrincipal(principalEmail))
                    .doOnNext(count -> log.info("Deleted {} sessions for user: {} (principal: {})", count, email, principalEmail))
                    .subscribe();
        }
    }
}
