package com.bardales.SmartLearnApi.dto.auth;

import java.time.LocalDateTime;

public record PresenceHeartbeatResponse(
        LocalDateTime serverTime,
        String message) {
}
