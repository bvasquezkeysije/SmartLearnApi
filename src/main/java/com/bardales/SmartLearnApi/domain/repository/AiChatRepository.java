package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.AiChat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatRepository extends JpaRepository<AiChat, Long> {
    List<AiChat> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AiChat> findByIdAndUserId(Long id, Long userId);
}