package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CoursePracticeWriteService {

    private final UserRepository userRepository;
    private final ExamService examService;
    private final ExamGroupPracticeService examGroupPracticeService;

    public CoursePracticeWriteService(
            UserRepository userRepository,
            ExamService examService,
            ExamGroupPracticeService examGroupPracticeService) {
        this.userRepository = userRepository;
        this.examService = examService;
        this.examGroupPracticeService = examGroupPracticeService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public void ensureParticipantAnchoredExamMembership(Exam sourceExam, Long userId) {
        assertWritableTx("ensureParticipantAnchoredExamMembership");
        if (sourceExam == null || sourceExam.getId() == null || userId == null) {
            return;
        }

        Long ownerUserId = sourceExam.getUser() == null ? null : sourceExam.getUser().getId();
        if (ownerUserId != null && ownerUserId.equals(userId)) {
            return;
        }

        User requester = userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        examService.upsertExamMembership(
                sourceExam,
                requester,
                "viewer",
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public ExamPracticeStartResponse startAnchoredExamPracticeAttempt(Exam sourceExam, Long userId) {
        assertWritableTx("startAnchoredExamPracticeAttempt");
        ensureParticipantAnchoredExamMembership(sourceExam, userId);
        return examService.startPracticeAttempt(sourceExam.getId(), userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public ExamGroupStateResponse joinAnchoredGroupPractice(Exam sourceExam, ExamGroupJoinRequest request) {
        assertWritableTx("joinAnchoredGroupPractice");
        ensureParticipantAnchoredExamMembership(sourceExam, request.userId());
        return examGroupPracticeService.join(sourceExam.getId(), request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public ExamGroupStateResponse createAnchoredGroupPractice(Exam sourceExam, ExamGroupJoinRequest request) {
        assertWritableTx("createAnchoredGroupPractice");
        ensureParticipantAnchoredExamMembership(sourceExam, request.userId());
        return examGroupPracticeService.create(sourceExam.getId(), request);
    }

    private void assertWritableTx(String methodName) {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("WRITE_IN_READONLY_TX:" + methodName);
        }
    }
}
