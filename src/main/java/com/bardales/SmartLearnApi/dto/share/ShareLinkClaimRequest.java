package com.bardales.SmartLearnApi.dto.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareLinkClaimRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "token es obligatorio") String token) {}
