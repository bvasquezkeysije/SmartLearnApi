package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoursePracticeWriteService {

    private final UserRepository userRepository;
    private final ExamService examService;

    public CoursePracticeWriteService(UserRepository userRepository, ExamService examService) {
        this.userRepository = userRepository;
        this.examService = examService;
    }

    @Transactional
    public void ensureParticipantAnchoredExamMembership(Exam sourceExam, Long userId) {
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
}
