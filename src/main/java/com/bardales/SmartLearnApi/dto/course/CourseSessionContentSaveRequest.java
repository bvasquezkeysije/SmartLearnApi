package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseSessionContentSaveRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String type,
        String title,
        String externalLink,
        String fileName,
        String fileData,
        Long weekId,
        Long sourceExamId) {}
