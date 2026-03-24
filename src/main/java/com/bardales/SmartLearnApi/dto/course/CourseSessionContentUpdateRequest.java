package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseSessionContentUpdateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String videoTitle,
        String videoLink,
        String coverTitle,
        String coverImageData,
        String pdfTitle,
        String pdfFileName,
        String pdfFileData,
        String wordTitle,
        String wordFileName,
        String wordFileData) {}
