package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ShareNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareNotificationRepository extends JpaRepository<ShareNotification, Long> {
    List<ShareNotification> findByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long recipientUserId);
    Optional<ShareNotification> findByIdAndRecipientUserIdAndDeletedAtIsNull(Long id, Long recipientUserId);
    Optional<ShareNotification> findTopByShareLinkIdAndRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long shareLinkId, Long recipientUserId);
    Optional<ShareNotification>
            findTopByShareLinkIdAndRecipientUserIdAndInvitationStatusIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(
                    Long shareLinkId, Long recipientUserId, String invitationStatus);
}
