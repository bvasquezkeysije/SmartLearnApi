package com.bardales.SmartLearnApi.dto.exam;

import java.time.LocalDateTime;

public record ExamGroupCurrentAnswerResponse(
        Long userId,
        String name,
        String username,
        String profileImageUrl,
        String selectedAnswer,
        String selectedOptionKey,
        Boolean correct,
        LocalDateTime answeredAt) {}
