package com.bardales.SmartLearnApi.dto.chat;

public record ChatGenerateExamResponse(
        Long examId,
        String examName,
        Integer questionsCount,
        ChatDetailResponse chat) {
}
