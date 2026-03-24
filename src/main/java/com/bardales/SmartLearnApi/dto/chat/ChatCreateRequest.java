package com.bardales.SmartLearnApi.dto.chat;

import jakarta.validation.constraints.NotNull;

public record ChatCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String firstMessage,
        String attachmentName) {
}
