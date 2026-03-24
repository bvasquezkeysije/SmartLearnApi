package com.bardales.SmartLearnApi.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportCallRequestCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "phoneNumber es obligatorio") String phoneNumber,
        String preferredSchedule,
        @NotBlank(message = "reason es obligatorio") String reason) {
}

