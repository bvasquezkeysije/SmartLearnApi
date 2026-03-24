package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Option;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByQuestionIdOrderByIdAsc(Long questionId);
    void deleteByQuestionId(Long questionId);
}