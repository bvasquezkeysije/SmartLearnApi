package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.SalaMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaMessageRepository extends JpaRepository<SalaMessage, Long> {
    List<SalaMessage> findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long salaId);
}
