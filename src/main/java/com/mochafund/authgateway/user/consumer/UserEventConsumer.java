package com.mochafund.authgateway.user.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochafund.authgateway.common.events.EventEnvelope;
import com.mochafund.authgateway.common.events.EventType;
import com.mochafund.authgateway.session.SessionsService;
import com.mochafund.authgateway.user.events.UserEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserEventConsumer {

    private final SessionsService sessions;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventType.USER_DELETED, groupId = "auth-gateway")
    public void handleUserDeleted(String message) {
        EventEnvelope<UserEventPayload> event = readEnvelope(message, UserEventPayload.class);
        String email = event.getPayload().getEmail();
        log.info("Processing user.deleted - User: {}", email);

        sessions.listByPrincipal(email)
            .doOnNext(sessionList -> log.info("Found {} sessions for user {}", sessionList.size(), email))
            .then(sessions.deleteAllByPrincipal(email))
            .doOnNext(count -> log.info("Deleted {} sessions for user: {}", count, email))
            .subscribe();
    }

    @KafkaListener(topics = EventType.USER_UPDATED, groupId = "auth-gateway")
    public void handleUserUpdated(String message) {
        EventEnvelope<UserEventPayload> event = readEnvelope(message, UserEventPayload.class);
        UserEventPayload payload = event.getPayload();
        String email = payload.getEmail();
        String oldEmail = payload.getOldEmail();
        boolean invalidate = payload.isInvalidate();

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

    private <T> EventEnvelope<T> readEnvelope(String message, Class<T> payloadType) {
        try {
            return objectMapper.readValue(
                    message,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse user event envelope", e);
        }
    }
}
