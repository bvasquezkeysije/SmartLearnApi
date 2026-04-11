package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamGroupJoinRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String roomSessionToken) {}
