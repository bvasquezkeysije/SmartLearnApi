package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<Exam> findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(String visibility);
    List<Exam> findByIdInAndUserIdAndDeletedAtIsNull(List<Long> ids, Long userId);
    List<Exam> findByIdInAndDeletedAtIsNull(List<Long> ids);
    Optional<Exam> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
    Optional<Exam> findByIdAndDeletedAtIsNull(Long id);
    Optional<Exam> findByUserIdAndSourceFilePathAndDeletedAtIsNull(Long userId, String sourceFilePath);
}
