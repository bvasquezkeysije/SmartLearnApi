package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.AndroidRelease;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidReleaseRepository extends JpaRepository<AndroidRelease, Long> {
    Optional<AndroidRelease> findByIsActiveTrue();
    List<AndroidRelease> findAllByOrderByCreatedAtDesc();
}
