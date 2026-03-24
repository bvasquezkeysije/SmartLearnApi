package com.bardales.SmartLearnApi.dto.auth;

import java.util.List;

public record GoogleLoginResponse(
        boolean requiresRegistration,
        Long id,
        String name,
        String username,
        String email,
        Integer status,
        List<String> roles,
        String token,
        String suggestedUsername,
        String message,
        String authProvider,
        Boolean hasLocalPassword) {
}
