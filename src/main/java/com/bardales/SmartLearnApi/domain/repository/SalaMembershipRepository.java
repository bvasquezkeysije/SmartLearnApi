package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.SalaMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaMembershipRepository extends JpaRepository<SalaMembership, Long> {
    List<SalaMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<SalaMembership> findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long salaId);
    Optional<SalaMembership> findBySalaIdAndUserIdAndDeletedAtIsNull(Long salaId, Long userId);
    Optional<SalaMembership> findBySalaIdAndUserId(Long salaId, Long userId);
}
