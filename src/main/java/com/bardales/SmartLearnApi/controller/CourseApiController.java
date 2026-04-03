package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.course.CourseCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseCompetencySaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseJoinRequest;
import com.bardales.SmartLearnApi.dto.course.CourseJoinResponse;
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
import com.bardales.SmartLearnApi.dto.course.CourseWeekContentReorderRequest;
import com.bardales.SmartLearnApi.dto.course.CourseWeekSaveRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAdvanceRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAnswerRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStartRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamRenameRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamIndividualPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantPermissionUpdateRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamSummaryResponse;
import com.bardales.SmartLearnApi.dto.exam.ManualQuestionUpsertRequest;
import com.bardales.SmartLearnApi.dto.exam.QuestionResponse;
import com.bardales.SmartLearnApi.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseApiController {

    private static final Logger log = LoggerFactory.getLogger(CourseApiController.class);

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

    @PostMapping("/{courseId}/join")
    public CourseJoinResponse joinPublicCourse(@PathVariable Long courseId, @Valid @RequestBody CourseJoinRequest request) {
        return courseService.joinPublicCourse(courseId, request.userId());
    }

    @PatchMapping("/{courseId}/join-requests/{requesterUserId}/accept")
    public CourseJoinResponse acceptJoinRequest(
            @PathVariable Long courseId,
            @PathVariable Long requesterUserId,
            @RequestParam Long userId) {
        return courseService.acceptCourseJoinRequest(courseId, requesterUserId, userId);
    }

    @PatchMapping("/{courseId}/join-requests/{requesterUserId}/reject")
    public CourseJoinResponse rejectJoinRequest(
            @PathVariable Long courseId,
            @PathVariable Long requesterUserId,
            @RequestParam Long userId) {
        return courseService.rejectCourseJoinRequest(courseId, requesterUserId, userId);
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

    @DeleteMapping("/{courseId}/sessions/{sessionId}")
    public CourseResponse deleteSession(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestParam Long userId) {
        return courseService.deleteCourseSession(courseId, sessionId, userId);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/weeks")
    public CourseResponse addWeek(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @Valid @RequestBody CourseWeekSaveRequest request) {
        return courseService.addCourseWeek(courseId, sessionId, request);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/weeks/{weekId}")
    public CourseResponse updateWeek(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long weekId,
            @Valid @RequestBody CourseWeekSaveRequest request) {
        return courseService.updateCourseWeek(courseId, sessionId, weekId, request);
    }

    @DeleteMapping("/{courseId}/sessions/{sessionId}/weeks/{weekId}")
    public CourseResponse deleteWeek(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long weekId,
            @RequestParam Long userId) {
        return courseService.deleteCourseWeek(courseId, sessionId, weekId, userId);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/weeks/{weekId}/contents/order")
    public CourseResponse reorderWeekContents(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long weekId,
            @Valid @RequestBody CourseWeekContentReorderRequest request) {
        return courseService.reorderWeekContents(courseId, sessionId, weekId, request);
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

    @DeleteMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}")
    public CourseResponse deleteSessionContent(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        return courseService.deleteCourseSessionContent(courseId, sessionId, contentId, userId);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/practice/start")
    public CourseSessionContentPracticeStartResponse startSessionContentPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody CourseSessionContentPracticeStartRequest request) {
        return courseService.startCourseSessionContentPractice(courseId, sessionId, contentId, request.userId());
    }

    @GetMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-summary")
    public ExamSummaryResponse getSessionContentExamSummary(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        return courseService.getCourseSessionContentExamSummary(courseId, sessionId, contentId, userId);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam/name")
    public ExamSummaryResponse renameSessionContentExam(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamRenameRequest request) {
        return courseService.renameCourseSessionContentExam(courseId, sessionId, contentId, request);
    }

    @DeleteMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam")
    public void deleteSessionContentExam(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        courseService.deleteCourseSessionContentExam(courseId, sessionId, contentId, userId);
    }

    @GetMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-questions")
    public List<QuestionResponse> getSessionContentExamQuestions(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        return courseService.getCourseSessionContentExamQuestions(courseId, sessionId, contentId, userId);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-questions")
    public QuestionResponse addSessionContentExamQuestion(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ManualQuestionUpsertRequest request) {
        return courseService.addCourseSessionContentExamQuestion(courseId, sessionId, contentId, request);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-questions/{questionId}")
    public QuestionResponse updateSessionContentExamQuestion(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @PathVariable Long questionId,
            @Valid @RequestBody ManualQuestionUpsertRequest request) {
        return courseService.updateCourseSessionContentExamQuestion(courseId, sessionId, contentId, questionId, request);
    }

    @GetMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-participants")
    public List<ExamParticipantResponse> getSessionContentExamParticipants(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        return courseService.getCourseSessionContentExamParticipants(courseId, sessionId, contentId, userId);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-participants/{participantUserId}")
    public void updateSessionContentExamParticipantPermissions(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @PathVariable Long participantUserId,
            @Valid @RequestBody ExamParticipantPermissionUpdateRequest request) {
        courseService.updateCourseSessionContentExamParticipantPermissions(
                courseId, sessionId, contentId, participantUserId, request);
    }

    @DeleteMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-participants/{participantUserId}")
    public void removeSessionContentExamParticipant(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @PathVariable Long participantUserId,
            @RequestParam Long requesterUserId) {
        courseService.removeCourseSessionContentExamParticipant(
                courseId, sessionId, contentId, participantUserId, requesterUserId);
    }

    @GetMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/settings/individual")
    public ExamPracticeSettingsResponse getSessionContentIndividualPracticeSettings(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId) {
        return courseService.getCourseSessionContentIndividualPracticeSettings(courseId, sessionId, contentId, userId);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/settings/individual")
    public ExamPracticeSettingsResponse updateSessionContentIndividualPracticeSettings(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamIndividualPracticeSettingsRequest request) {
        return courseService.updateCourseSessionContentIndividualPracticeSettings(courseId, sessionId, contentId, request);
    }

    @PatchMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/settings")
    public ExamSummaryResponse updateSessionContentPracticeSettings(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamPracticeSettingsRequest request) {
        return courseService.updateCourseSessionContentPracticeSettings(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/start")
    public ExamPracticeStartResponse startSessionContentExamPracticeAttempt(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody CourseSessionContentPracticeStartRequest request) {
        log.info("COURSE_EXAM_PRACTICE_START courseId={} sessionId={} contentId={} userId={}",
            courseId, sessionId, contentId, request.userId());
        return courseService.startCourseSessionContentExamPracticeAttempt(courseId, sessionId, contentId, request.userId());
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/join")
    public ExamGroupStateResponse joinSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupJoinRequest request) {
        log.info("COURSE_EXAM_GROUP_JOIN courseId={} sessionId={} contentId={} userId={}",
            courseId, sessionId, contentId, request.userId());
        return courseService.joinCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/create")
    public ExamGroupStateResponse createSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupJoinRequest request) {
        log.info("COURSE_EXAM_GROUP_CREATE courseId={} sessionId={} contentId={} userId={}",
            courseId, sessionId, contentId, request.userId());
        return courseService.createCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/start")
    public ExamGroupStateResponse startSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupStartRequest request) {
        return courseService.startCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @GetMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/state")
    public ExamGroupStateResponse getSessionContentGroupPracticeState(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @RequestParam Long userId,
            @RequestParam Long sessionGroupId) {
        return courseService.getCourseSessionContentGroupPracticeState(courseId, sessionId, contentId, sessionGroupId, userId);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/answer")
    public ExamGroupStateResponse answerSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupAnswerRequest request) {
        return courseService.answerCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/next")
    public ExamGroupStateResponse nextSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return courseService.nextCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/close")
    public ExamGroupStateResponse closeSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return courseService.closeCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
    }

    @PostMapping("/{courseId}/sessions/{sessionId}/contents/{contentId}/exam-practice/group/restart")
    public ExamGroupStateResponse restartSessionContentGroupPractice(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long contentId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return courseService.restartCourseSessionContentGroupPractice(courseId, sessionId, contentId, request);
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
