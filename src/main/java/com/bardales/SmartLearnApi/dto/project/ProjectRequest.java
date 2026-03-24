package com.bardales.SmartLearnApi.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectRequest(
        @NotNull(message = "requesterUserId es obligatorio") Long requesterUserId,
        Long ownerUserId,
        @NotBlank(message = "name es obligatorio") String name,
        String description) {
}