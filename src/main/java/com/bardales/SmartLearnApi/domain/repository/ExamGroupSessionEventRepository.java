package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamGroupSessionEventRepository extends JpaRepository<ExamGroupSessionEvent, Long> {
}

