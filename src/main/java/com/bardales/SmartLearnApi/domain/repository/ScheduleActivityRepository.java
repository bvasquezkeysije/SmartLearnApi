package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ScheduleActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleActivityRepository extends JpaRepository<ScheduleActivity, Long> {
    List<ScheduleActivity> findByScheduleProfileIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long scheduleProfileId);
    Optional<ScheduleActivity> findByIdAndScheduleProfileIdAndDeletedAtIsNull(Long id, Long scheduleProfileId);
}
