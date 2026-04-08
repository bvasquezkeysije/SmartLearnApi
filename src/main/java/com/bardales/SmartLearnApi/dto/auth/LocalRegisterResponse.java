package com.bardales.SmartLearnApi.dto.auth;

import java.util.List;

public record LocalRegisterResponse(
        Long id,
        String name,
        String username,
        String email,
        Integer status,
        List<String> roles,
        String token,
        String authProvider,
        Boolean hasLocalPassword,
        String message,
        String profileImageData,
        Double profileImageScale,
        Double profileImageOffsetX,
        Double profileImageOffsetY) {
}
