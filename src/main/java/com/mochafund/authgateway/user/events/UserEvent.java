package com.mochafund.authgateway.user.events;

import com.mochafund.authgateway.common.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class UserEvent extends BaseEvent {
    private Data data;

    @Builder
    public record Data (
            UUID userId,
            String email,
            String givenName,
            String familyName,
            Boolean isActive,
            UUID lastWorkspaceId,
            boolean invalidate
    ) {}
}