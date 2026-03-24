package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDeletedAtIsNullOrderByCreatedAtDesc();
    List<Task> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long projectId);
    Optional<Task> findByIdAndDeletedAtIsNull(Long id);
}