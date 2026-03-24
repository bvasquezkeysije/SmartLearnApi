package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ShareLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByTokenAndActiveIsTrueAndDeletedAtIsNull(String token);
}
