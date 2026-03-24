package com.bardales.SmartLearnApi.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskRequest(
        @NotNull(message = "requesterUserId es obligatorio") Long requesterUserId,
        @NotNull(message = "projectId es obligatorio") Long projectId,
        @NotBlank(message = "title es obligatorio") String title,
        @NotBlank(message = "status es obligatorio") String status,
        @NotBlank(message = "priority es obligatorio") String priority) {
}