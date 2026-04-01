package com.bardales.SmartLearnApi.dto.exam;

public record ExamListVisibilityResponse(
        Long examId,
        Long userId,
        boolean visible) {}
