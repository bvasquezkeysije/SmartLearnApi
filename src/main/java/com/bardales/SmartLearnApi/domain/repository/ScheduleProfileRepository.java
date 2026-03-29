package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ScheduleProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleProfileRepository extends JpaRepository<ScheduleProfile, Long> {
    List<ScheduleProfile> findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long ownerUserId);
    Optional<ScheduleProfile> findByIdAndDeletedAtIsNull(Long id);
    Optional<ScheduleProfile> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);
}
