package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.exam.ExamRenameRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamIndividualPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAdvanceRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAnswerRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStartRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantPermissionUpdateRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamSummaryResponse;
import com.bardales.SmartLearnApi.dto.exam.ManualExamCreateRequest;
import com.bardales.SmartLearnApi.dto.exam.ManualQuestionUpsertRequest;
import com.bardales.SmartLearnApi.dto.exam.QuestionResponse;
import com.bardales.SmartLearnApi.service.ExamService;
import com.bardales.SmartLearnApi.service.ExamGroupPracticeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ia/exams")
public class ExamApiController {

    private final ExamService examService;
    private final ExamGroupPracticeService examGroupPracticeService;

    public ExamApiController(ExamService examService, ExamGroupPracticeService examGroupPracticeService) {
        this.examService = examService;
        this.examGroupPracticeService = examGroupPracticeService;
    }

    @GetMapping
    public List<ExamSummaryResponse> listExams(@RequestParam Long userId) {
        return examService.listExams(userId);
    }

    @PostMapping("/manual")
    public ExamSummaryResponse createManual(@Valid @RequestBody ManualExamCreateRequest request) {
        return examService.createManualExam(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExamSummaryResponse uploadExam(
            @RequestParam Long userId,
            @RequestParam String examName,
            @RequestParam("examFile") MultipartFile examFile) {
        return examService.importExam(userId, examName, examFile);
    }

    @GetMapping("/format")
    public ResponseEntity<ByteArrayResource> downloadFormat() {
        return buildFormatResponse();
    }

    @GetMapping("/format/v2")
    public ResponseEntity<ByteArrayResource> downloadFormatV2() {
        return buildFormatResponse();
    }

    private ResponseEntity<ByteArrayResource> buildFormatResponse() {
        byte[] file = examService.downloadExamFormatTemplate();
        ByteArrayResource resource = new ByteArrayResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=formato_examen_a21k_v2.xlsx")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(resource);
    }

    @PatchMapping("/{examId}/name")
    public ExamSummaryResponse rename(@PathVariable Long examId, @Valid @RequestBody ExamRenameRequest request) {
        return examService.renameExam(examId, request);
    }

    @PatchMapping("/{examId}/practice/settings")
    public ExamSummaryResponse updatePracticeSettings(
            @PathVariable Long examId,
            @Valid @RequestBody ExamPracticeSettingsRequest request) {
        return examService.updatePracticeSettings(examId, request);
    }

    @GetMapping("/{examId}/practice/settings/individual")
    public ExamPracticeSettingsResponse getIndividualPracticeSettings(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        return examService.getIndividualPracticeSettings(examId, userId);
    }

    @PatchMapping("/{examId}/practice/settings/individual")
    public ExamPracticeSettingsResponse updateIndividualPracticeSettings(
            @PathVariable Long examId,
            @Valid @RequestBody ExamIndividualPracticeSettingsRequest request) {
        return examService.updateIndividualPracticeSettings(examId, request);
    }

    @PostMapping("/{examId}/practice/start")
    public ExamPracticeStartResponse startPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamPracticeStartRequest request) {
        return examService.startPracticeAttempt(examId, request.userId());
    }

    @PostMapping("/{examId}/practice/group/join")
    public ExamGroupStateResponse joinGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupJoinRequest request) {
        return examGroupPracticeService.join(examId, request);
    }

    @PostMapping("/{examId}/practice/group/create")
    public ExamGroupStateResponse createGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupJoinRequest request) {
        return examGroupPracticeService.create(examId, request);
    }

    @PostMapping("/{examId}/practice/group/start")
    public ExamGroupStateResponse startGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupStartRequest request) {
        return examGroupPracticeService.start(examId, request);
    }

    @GetMapping("/{examId}/practice/group/state")
    public ExamGroupStateResponse groupPracticeState(
            @PathVariable Long examId,
            @RequestParam Long userId,
            @RequestParam Long sessionId) {
        return examGroupPracticeService.state(examId, sessionId, userId);
    }

    @PostMapping("/{examId}/practice/group/answer")
    public ExamGroupStateResponse answerGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupAnswerRequest request) {
        return examGroupPracticeService.answer(examId, request);
    }

    @PostMapping("/{examId}/practice/group/next")
    public ExamGroupStateResponse nextGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return examGroupPracticeService.next(examId, request);
    }

    @PostMapping("/{examId}/practice/group/close")
    public ExamGroupStateResponse closeGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return examGroupPracticeService.close(examId, request);
    }

    @PostMapping("/{examId}/practice/group/restart")
    public ExamGroupStateResponse restartGroupPractice(
            @PathVariable Long examId,
            @Valid @RequestBody ExamGroupAdvanceRequest request) {
        return examGroupPracticeService.restart(examId, request);
    }

    @GetMapping("/{examId}/manual")
    public List<QuestionResponse> showManual(@PathVariable Long examId, @RequestParam Long userId) {
        return examService.getManualExam(examId, userId);
    }

    @GetMapping("/{examId}/participants")
    public List<ExamParticipantResponse> listParticipants(@PathVariable Long examId, @RequestParam Long userId) {
        return examService.listParticipants(examId, userId);
    }

    @PatchMapping("/{examId}/participants/{participantUserId}")
    public void updateParticipantPermissions(
            @PathVariable Long examId,
            @PathVariable Long participantUserId,
            @Valid @RequestBody ExamParticipantPermissionUpdateRequest request) {
        examService.updateExamParticipantPermissions(examId, participantUserId, request);
    }

    @DeleteMapping("/{examId}/participants/{participantUserId}")
    public void removeParticipant(
            @PathVariable Long examId,
            @PathVariable Long participantUserId,
            @RequestParam Long requesterUserId) {
        examService.removeExamParticipant(examId, participantUserId, requesterUserId);
    }

    @PostMapping("/{examId}/manual/questions")
    public QuestionResponse addQuestion(@PathVariable Long examId, @Valid @RequestBody ManualQuestionUpsertRequest request) {
        return examService.addManualQuestion(examId, request);
    }

    @PatchMapping("/{examId}/manual/questions/{questionId}")
    public QuestionResponse updateQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody ManualQuestionUpsertRequest request) {
        return examService.updateManualQuestion(examId, questionId, request);
    }

    @DeleteMapping("/{examId}")
    public void deleteExam(@PathVariable Long examId, @RequestParam Long userId) {
        examService.deleteExam(examId, userId);
    }
}
