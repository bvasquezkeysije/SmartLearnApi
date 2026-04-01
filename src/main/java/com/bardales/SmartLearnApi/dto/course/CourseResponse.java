package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;
import java.util.List;

public record CourseResponse(
        Long id,
        String name,
        String description,
        String coverImageData,
        String code,
        String visibility,
        String joinMode,
        String priority,
        Integer sortOrder,
        Long ownerUserId,
        List<CourseSessionItemResponse> sessions,
        List<CourseExamItemResponse> exams,
        List<CourseParticipantItemResponse> participants,
        List<CourseGradeItemResponse> grades,
        List<CourseCompetencyItemResponse> competencies,
        LocalDateTime createdAt) {
}
