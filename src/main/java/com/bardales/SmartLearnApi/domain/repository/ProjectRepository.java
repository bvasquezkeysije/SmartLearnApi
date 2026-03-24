package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByDeletedAtIsNullOrderByCreatedAtDesc();
    List<Project> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    Optional<Project> findByIdAndDeletedAtIsNull(Long id);
}