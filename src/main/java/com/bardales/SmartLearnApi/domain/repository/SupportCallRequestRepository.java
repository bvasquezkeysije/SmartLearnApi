package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.SupportCallRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportCallRequestRepository extends JpaRepository<SupportCallRequest, Long> {
    List<SupportCallRequest> findByRequesterUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long requesterUserId);
    List<SupportCallRequest> findByDeletedAtIsNullOrderByCreatedAtDesc();
}

