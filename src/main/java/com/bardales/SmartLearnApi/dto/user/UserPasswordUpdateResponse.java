package com.bardales.SmartLearnApi.dto.user;

public record UserPasswordUpdateResponse(
        String message,
        Boolean hasLocalPassword) {
}
