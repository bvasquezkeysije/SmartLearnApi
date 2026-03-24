package com.bardales.SmartLearnApi.dto.exam;

public record QuestionResponse(
        Long id,
        Long examId,
        String questionText,
        String questionType,
        String correctAnswer,
        String explanation,
        Integer points,
        Integer temporizadorSegundos,
        Integer reviewSeconds,
        Boolean timerEnabled,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption) {
}