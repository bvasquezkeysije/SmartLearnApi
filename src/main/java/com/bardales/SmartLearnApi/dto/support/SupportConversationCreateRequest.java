package com.bardales.SmartLearnApi.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportConversationCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "subject es obligatorio") String subject,
        String priority,
        String channelPreference,
        String whatsappNumber,
        String callNumber,
        @NotBlank(message = "initialMessage es obligatorio") String initialMessage) {
}

