package com.mochafund.authgateway.user.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEventPayload {
    private UUID userId;
    private String email;
    private String oldEmail;
    private Boolean isActive;
    private UUID lastWorkspaceId;
    private boolean invalidate;
}
