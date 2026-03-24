package com.bardales.SmartLearnApi.dto.course;

public record CourseExamItemResponse(
        Long id,
        String name,
        Integer questionsCount) {
}
