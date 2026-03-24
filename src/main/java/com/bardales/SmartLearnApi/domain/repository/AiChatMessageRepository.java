package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.AiChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);
    long countByChatId(Long chatId);
}
