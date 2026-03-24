package com.bardales.SmartLearnApi.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "content es obligatorio") String content,
        String attachmentName) {
}