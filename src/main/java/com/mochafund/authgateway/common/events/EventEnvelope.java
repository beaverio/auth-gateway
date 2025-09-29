package com.mochafund.authgateway.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime occurredAt = LocalDateTime.now();

    private UUID correlationId;
    private String type;

    @Builder.Default
    private int version = 1;

    private String actor;
    private T payload;
}
