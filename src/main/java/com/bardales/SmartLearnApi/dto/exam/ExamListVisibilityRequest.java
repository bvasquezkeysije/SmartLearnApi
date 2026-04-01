package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamListVisibilityRequest(
        @NotNull Long userId,
        @NotNull Boolean visible) {}
