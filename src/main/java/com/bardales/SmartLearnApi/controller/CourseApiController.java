package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.course.CourseCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseCompetencySaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseModuleResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantRoleUpdateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentPracticeStartRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionUpdateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSetExamsRequest;
import com.bardales.SmartLearnApi.dto.course.CourseUpdateRequest;
import com.bardales.SmartLearnApi.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseApiController {

    private final CourseService courseService;

    public CourseApiController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public CourseModuleResponse getModule(@RequestParam Long userId) {
        return courseService.getModule(userId);
    }

    @PostMapping
    public CourseResponse create(@Valid @RequestBody CourseCreateRequest request) {
        return courseService.createCourse(request);
    }

    @PostMapping("/{courseId}/participants")
    public CourseResponse addParticipant(@PathVariable Long courseId, @Valid @RequestBody CourseParticipantSaveRequest request) {
        return courseService.addCourseParticipant(courseId, request);
    }

    @PatchMapping("/{courseId}/participants/{participantUserId}")
    public CourseResponse updateParticipantRole(
            @PathVariable Long courseId,
            @PathVariable Long participantUserId,
            @Valid @RequestBody CourseParticipantRoleUpdateRequest request) {
        return courseService.updateCourseParticipantRole(courseId, participantUserId, request);
    }

    @DeleteMapping("/{courseId}/participants/{participantUserId}")
    public CourseResponse removeParticipant(
            @PathVariable Long courseId,
            @PathVariable Long participantUserId,
            @RequestParam Long userId) {
        return courseService.removeCourseParticipant(courseId, participantUserId, userId);
    }

    @PostMapping("/{courseId}/competencies")
    public CourseResponse addCompetency(@PathVariable Long courseId, @Valid @RequestBody CourseCompetencySaveRequest request) {
        return courseService.addCourseCompetency(courseId, request);
    }

    @PatchMapping("/{courseId}/competencies/{competencyId}")
    public CourseResponse updateCompetency(
            @PathVariable Long courseId,
            @PathVariable Long competencyId,
            @Valid @RequestBody CourseCompetencySaveRequest request) {
        return courseService.updateCourseCompetency(courseId, competencyId, request);
    }

    @DeleteMapping("/{courseId}/competencies/{competencyId}")
    public CourseResponse deleteCompetency(
            @PathVariable Long courseId,
            @PathVariable Long competencyId,
            @RequestParam Long userId) {
        return courseService.deleteCourseCompetency(courseId, competencyId, userId);
    }

    @PutMapping("/{courseId}/exams")
    public CourseResponse setExams(@PathVariable Long courseId, @Valid @RequestBody CourseSetExamsRequest request) {
        return courseService.setCourseExams(courseId, request);
    }

    @PostMapping("/{courseId}/sessions")
    public CourseResponse createSession(
            @PathVariable Long courseId, @Valid @RequestBody CourseSessionCreateRequest request) {
        return courseService.createCourseSession(courseId, request);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}")
    public CourseResponse updateSession(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @Valid @RequestBody CourseSessionUpdateRequest request) {
        return courseService.updateCourseSession(courseId, sessionId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents")
    public CourseResponse addSessionContent(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @Valid @RequestBody CourseSessionContentSaveRequest request) {
        return courseService.addCourseSessionContent(courseId, sessionId, request);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}")
    public CourseResponse updateSessionContent(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody CourseSessionContentSaveRequest request) {
        return courseService.updateCourseSessionContent(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/practice/start")
    public CourseSessionContentPracticeStartResponse startSessionContentPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody CourseSessionContentPracticeStartRequest request) {
        return courseService.startCourseSessionContentPractice(courseId, sessionId, contentId, request.userId());
    }

    @PatchMapping("/{courseId}")
    public CourseResponse update(@PathVariable Long courseId, @Valid @RequestBody CourseUpdateRequest request) {
        return courseService.updateCourse(courseId, request);
    }

    @DeleteMapping("/{courseId}")
    public void delete(@PathVariable Long courseId, @RequestParam Long userId) {
        courseService.deleteCourse(courseId, userId);
    }
}
