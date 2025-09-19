package com.mochafund.authgateway.user.consumer;

import com.mochafund.authgateway.user.events.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserEventConsumer {

    @KafkaListener(topics = "user.deleted", groupId = "auth-gateway")
    public void handleUserDeleted(UserEvent event) {
        log.info("Processing user.deleted - User: {}",
                event.getData().email());
    }

    @KafkaListener(topics = "user.updated", groupId = "auth-gateway")
    public void handleUserUpdated(UserEvent event) {
        log.info("Processing user.updated - User: {}",
                event.getData().email());
    }
}
