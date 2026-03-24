package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.SupportConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {
    List<SupportConversation> findByRequesterUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long requesterUserId);
    List<SupportConversation> findByDeletedAtIsNullOrderByUpdatedAtDesc();
    Optional<SupportConversation> findByIdAndDeletedAtIsNull(Long id);
}

