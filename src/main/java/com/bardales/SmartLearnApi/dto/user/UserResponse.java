package com.bardales.SmartLearnApi.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String name,
        String username,
        String email,
        Integer status,
        Boolean online,
        LocalDateTime lastSeenAt,
        List<String> roles) {
}