package com.bardales.SmartLearnApi.dto.auth;

import java.util.List;

public record LoginResponse(
        Long id,
        String name,
        String username,
        String email,
        Integer status,
        List<String> roles,
        String token,
        String authProvider,
        Boolean hasLocalPassword,
        String profileImageData,
        Double profileImageScale,
        Double profileImageOffsetX,
        Double profileImageOffsetY) {
}
