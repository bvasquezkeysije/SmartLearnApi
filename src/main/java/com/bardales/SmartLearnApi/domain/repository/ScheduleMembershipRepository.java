package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ScheduleMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleMembershipRepository extends JpaRepository<ScheduleMembership, Long> {
    List<ScheduleMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<ScheduleMembership> findByScheduleProfileIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long scheduleProfileId);
    Optional<ScheduleMembership> findByScheduleProfileIdAndUserIdAndDeletedAtIsNull(Long scheduleProfileId, Long userId);
    Optional<ScheduleMembership> findByScheduleProfileIdAndUserId(Long scheduleProfileId, Long userId);
}
