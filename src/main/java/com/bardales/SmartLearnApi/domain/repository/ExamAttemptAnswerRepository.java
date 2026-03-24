package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswer, Long> {
}