package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamPracticePreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPracticePreferenceRepository extends JpaRepository<ExamPracticePreference, Long> {
    Optional<ExamPracticePreference> findByExamIdAndUserIdAndDeletedAtIsNull(Long examId, Long userId);
}
