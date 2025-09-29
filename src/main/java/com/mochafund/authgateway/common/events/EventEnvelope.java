package com.mochafund.authgateway.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventEnvelope<T> {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private UUID correlationId;
    private String type;

    @Builder.Default
    private int version = 1;

    private String actor;
    private T payload;
}
