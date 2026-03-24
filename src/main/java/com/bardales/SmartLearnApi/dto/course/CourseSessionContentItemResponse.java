package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;

public record CourseSessionContentItemResponse(
        Long id,
        String type,
        String title,
        String externalLink,
        String fileName,
        String fileData,
        Long sourceExamId,
        String sourceExamName,
        LocalDateTime createdAt) {}
