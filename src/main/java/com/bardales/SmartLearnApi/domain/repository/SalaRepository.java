package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Sala;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    List<Sala> findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long ownerUserId);
    Optional<Sala> findByIdAndDeletedAtIsNull(Long id);
    Optional<Sala> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);
    Optional<Sala> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);
    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);
    boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}
