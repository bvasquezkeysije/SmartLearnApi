package com.bardales.SmartLearnApi.dto.exam;

public record ExamGroupRankingEntryResponse(
        Integer rank,
        Long userId,
        String name,
        String username,
        String profileImageUrl,
        Integer correctCount,
        Integer wrongCount,
        Integer baseScore,
        Integer speedBonus,
        Integer finalScore) {}
