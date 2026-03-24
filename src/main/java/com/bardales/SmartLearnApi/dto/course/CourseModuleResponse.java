package com.bardales.SmartLearnApi.dto.course;

import java.util.List;

public record CourseModuleResponse(
        List<CourseResponse> courses,
        List<CourseExamItemResponse> availableExams) {
}
