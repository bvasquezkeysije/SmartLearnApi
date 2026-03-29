package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Course;
import com.bardales.SmartLearnApi.domain.entity.CourseMembership;
import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import com.bardales.SmartLearnApi.domain.entity.Sala;
import com.bardales.SmartLearnApi.domain.entity.ScheduleProfile;
import com.bardales.SmartLearnApi.domain.entity.ShareLink;
import com.bardales.SmartLearnApi.domain.entity.ShareNotification;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.CourseMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.ShareLinkRepository;
import com.bardales.SmartLearnApi.domain.repository.ShareNotificationRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimResponse;
import com.bardales.SmartLearnApi.dto.share.ShareLinkCreateRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkDistributeRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkDistributeResponse;
import com.bardales.SmartLearnApi.dto.share.ShareNotificationRecipientResponse;
import com.bardales.SmartLearnApi.dto.share.ShareNotificationResponse;
import com.bardales.SmartLearnApi.dto.share.ShareLinkResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareLinkService {

    private static final int DEFAULT_EXPIRES_HOURS = 24 * 7;
    private static final int MAX_EXPIRES_HOURS = 24 * 30;
    private static final int DEFAULT_RECIPIENTS_LIMIT = 20;
    private static final int MAX_RECIPIENTS_LIMIT = 50;
    private static final int MIN_RECIPIENTS_QUERY_LENGTH = 2;
    private static final String INVITATION_STATUS_PENDING = "pending";
    private static final String INVITATION_STATUS_ACCEPTED = "accepted";
    private static final String INVITATION_STATUS_REJECTED = "rejected";

    private final ShareLinkRepository shareLinkRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamMembershipRepository examMembershipRepository;
    private final CourseRepository courseRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final ShareNotificationRepository shareNotificationRepository;
    private final ExamService examService;
    private final ScheduleService scheduleService;
    private final SalaService salaService;

    public ShareLinkService(
            ShareLinkRepository shareLinkRepository,
            UserRepository userRepository,
            ExamRepository examRepository,
            ExamMembershipRepository examMembershipRepository,
            CourseRepository courseRepository,
            CourseMembershipRepository courseMembershipRepository,
            ShareNotificationRepository shareNotificationRepository,
            ExamService examService,
            ScheduleService scheduleService,
            SalaService salaService) {
        this.shareLinkRepository = shareLinkRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.examMembershipRepository = examMembershipRepository;
        this.courseRepository = courseRepository;
        this.courseMembershipRepository = courseMembershipRepository;
        this.shareNotificationRepository = shareNotificationRepository;
        this.examService = examService;
        this.scheduleService = scheduleService;
        this.salaService = salaService;
    }

    @Transactional
    public ShareLinkResponse createExamShareLink(Long examId, ShareLinkCreateRequest request) {
        User owner = requireUser(request.userId());
        Exam exam = examService.requireExamCanShare(examId, owner.getId());
        ShareLink shareLink = createShareLink(owner, "exam", exam.getId(), request.expiresInHours());
        return toResponse(shareLink);
    }

    @Transactional
    public ShareLinkResponse createCourseShareLink(Long courseId, ShareLinkCreateRequest request) {
        User owner = requireUser(request.userId());
        Course course = courseRepository.findByIdAndUserIdAndDeletedAtIsNull(courseId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Curso no encontrado"));
        ShareLink shareLink = createShareLink(owner, "course", course.getId(), request.expiresInHours());
        return toResponse(shareLink);
    }

    @Transactional
    public ShareLinkResponse createScheduleShareLink(Long scheduleId, ShareLinkCreateRequest request) {
        User owner = requireUser(request.userId());
        scheduleService.requireScheduleCanShare(scheduleId, owner.getId());
        ShareLink shareLink = createShareLink(owner, "schedule", scheduleId, request.expiresInHours());
        return toResponse(shareLink);
    }

    @Transactional
    public ShareLinkResponse createSalaShareLink(Long salaId, ShareLinkCreateRequest request) {
        User owner = requireUser(request.userId());
        salaService.requireSalaCanShare(salaId, owner.getId());
        ShareLink shareLink = createShareLink(owner, "sala", salaId, request.expiresInHours());
        return toResponse(shareLink);
    }

    @Transactional
    public ShareLinkClaimResponse claimShareLink(ShareLinkClaimRequest request) {
        User user = requireUser(request.userId());
        String token = normalizeToken(request.token());
        ShareLink shareLink = shareLinkRepository
                .findByTokenAndActiveIsTrueAndDeletedAtIsNull(token)
                .orElseThrow(() -> new NotFoundException("Enlace de comparticion no encontrado"));

        validateShareLinkIsUsable(shareLink);
        String type = normalizeResourceType(shareLink.getResourceType());

        ShareLinkClaimResponse response;
        if (type.equals("exam")) {
            response = claimExamLink(shareLink, user);
        } else if (type.equals("course")) {
            response = claimCourseLink(shareLink, user);
        } else if (type.equals("schedule")) {
            response = claimScheduleLink(shareLink, user);
        } else if (type.equals("sala")) {
            response = claimSalaLink(shareLink, user);
        } else {
            throw new BadRequestException("Tipo de recurso de comparticion invalido");
        }

        shareLink.setClaimsCount((shareLink.getClaimsCount() == null ? 0 : shareLink.getClaimsCount()) + 1);
        shareLinkRepository.save(shareLink);
        return response;
    }

    private ShareLink createShareLink(User owner, String resourceType, Long resourceId, Integer expiresInHours) {
        String normalizedResourceType = normalizeResourceType(resourceType);
        ShareLink existing = shareLinkRepository
                .findTopByOwnerUserIdAndResourceTypeIgnoreCaseAndResourceIdAndActiveIsTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
                        owner.getId(),
                        normalizedResourceType,
                        resourceId)
                .orElse(null);

        if (existing != null && isShareLinkCurrentlyUsable(existing)) {
            return existing;
        }

        ShareLink shareLink = new ShareLink();
        shareLink.setOwnerUser(owner);
        shareLink.setResourceType(normalizedResourceType);
        shareLink.setResourceId(resourceId);
        shareLink.setToken(generateShareToken());
        shareLink.setExpiresAt(LocalDateTime.now().plusHours(resolveExpiresInHours(expiresInHours)));
        shareLink.setMaxClaims(null);
        shareLink.setClaimsCount(0);
        shareLink.setActive(Boolean.TRUE);
        return shareLinkRepository.save(shareLink);
    }

    private ShareLinkClaimResponse claimExamLink(ShareLink shareLink, User user) {
        Exam exam = examRepository.findById(shareLink.getResourceId())
                .orElseThrow(() -> new NotFoundException("Examen no encontrado"));
        if (exam.getDeletedAt() != null) {
            throw new NotFoundException("Examen no encontrado");
        }

        Long ownerUserId = exam.getUser() == null ? null : exam.getUser().getId();
        boolean isOwner = ownerUserId != null && ownerUserId.equals(user.getId());
        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), user.getId())
                .orElse(null);

        if (!isOwner && membership == null) {
            String visibility = normalizeExamVisibility(exam.getVisibility());
            if (visibility.equals("public")) {
                examService.upsertExamMembership(exam, user, "viewer", Boolean.FALSE);
            } else {
                ShareNotification acceptedInvitation = shareNotificationRepository
                        .findTopByShareLinkIdAndRecipientUserIdAndInvitationStatusIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(
                                shareLink.getId(),
                                user.getId(),
                                INVITATION_STATUS_ACCEPTED)
                        .orElse(null);
                if (acceptedInvitation == null) {
                    throw new BadRequestException("Debes aceptar la invitacion desde tu bandeja para acceder a este examen.");
                }
                examService.upsertExamMembership(
                        exam,
                        user,
                        normalizeExamRole(acceptedInvitation.getExamRole()),
                        Boolean.TRUE.equals(acceptedInvitation.getExamCanShare()));
            }
        }

        String examName = trimOrNull(exam.getName());
        if (examName == null) {
            examName = "examen";
        }

        return new ShareLinkClaimResponse(
                "exam",
                exam.getId(),
                examName,
                "Examen compartido disponible en tu modulo de examenes.");
    }

    private ShareLinkClaimResponse claimCourseLink(ShareLink shareLink, User user) {
        Course course = courseRepository.findById(shareLink.getResourceId())
                .orElseThrow(() -> new NotFoundException("Curso no encontrado"));
        if (course.getDeletedAt() != null) {
            throw new NotFoundException("Curso no encontrado");
        }

        Long ownerId = course.getUser() == null ? null : course.getUser().getId();
        if (ownerId == null) {
            throw new NotFoundException("Curso no encontrado");
        }

        if (!ownerId.equals(user.getId())) {
            CourseMembership membership = courseMembershipRepository
                    .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), user.getId())
                    .orElse(null);
            if (membership == null) {
                membership = new CourseMembership();
                membership.setCourse(course);
                membership.setUser(user);
                membership.setRole("viewer");
                courseMembershipRepository.save(membership);
            }
        }

        String courseName = trimOrNull(course.getName());
        if (courseName == null) {
            courseName = "curso";
        }
        return new ShareLinkClaimResponse(
                "course",
                course.getId(),
                courseName,
                "Curso compartido agregado a tu modulo de cursos.");
    }

    private ShareLinkClaimResponse claimSalaLink(ShareLink shareLink, User user) {
        Sala sala = salaService.requireSala(shareLink.getResourceId());

        Long ownerUserId = sala.getOwnerUser() == null ? null : sala.getOwnerUser().getId();
        boolean isOwner = ownerUserId != null && ownerUserId.equals(user.getId());
        if (!isOwner) {
            salaService.upsertSalaMembership(sala, user, "viewer", false);
        }

        String salaName = trimOrNull(sala.getName());
        if (salaName == null) {
            salaName = "Sala";
        }

        return new ShareLinkClaimResponse(
                "sala",
                sala.getId(),
                salaName,
                "Sala compartida lista en tu modulo de salas.");
    }

    private ShareLinkClaimResponse claimScheduleLink(ShareLink shareLink, User user) {
        ScheduleProfile scheduleProfile = scheduleService.requireScheduleProfile(shareLink.getResourceId());

        Long ownerUserId = scheduleProfile.getOwnerUser() == null ? null : scheduleProfile.getOwnerUser().getId();
        boolean isOwner = ownerUserId != null && ownerUserId.equals(user.getId());
        if (!isOwner) {
            scheduleService.upsertScheduleMembership(scheduleProfile, user, "viewer", false);
        }

        String scheduleName = trimOrNull(scheduleProfile.getName());
        if (scheduleName == null) {
            scheduleName = "horario";
        }

        return new ShareLinkClaimResponse(
                "schedule",
                scheduleProfile.getId(),
                scheduleName,
                "Horario compartido agregado a tu modulo de horarios.");
    }

    @Transactional(readOnly = true)
    public List<ShareNotificationRecipientResponse> listRecipients(Long userId, String query, Integer limit) {
        User requester = requireUser(userId);
        String normalizedQuery = trimOrNull(query);
        if (normalizedQuery == null || normalizedQuery.length() < MIN_RECIPIENTS_QUERY_LENGTH) {
            return List.of();
        }

        int safeLimit = resolveRecipientsLimit(limit);
        return userRepository
                .searchActiveRecipients(
                        1,
                        requester.getId(),
                        normalizedQuery,
                        PageRequest.of(0, safeLimit))
                .stream()
                .filter(user -> user.getId() != null)
                .map(user -> new ShareNotificationRecipientResponse(
                        user.getId(),
                        trimOrNull(user.getName()) == null ? "Usuario" : trimOrNull(user.getName()),
                        trimOrNull(user.getUsername()) == null ? "" : trimOrNull(user.getUsername()),
                        trimOrNull(user.getEmail()) == null ? "" : trimOrNull(user.getEmail())))
                .toList();
    }

    @Transactional
    public ShareLinkDistributeResponse distributeShareLink(ShareLinkDistributeRequest request) {
        User owner = requireUser(request.userId());
        String resourceType = normalizeDistributionResourceType(request.resourceType());
        Long resourceId = request.resourceId();

        if (resourceId == null || resourceId <= 0) {
            throw new BadRequestException("resourceId es obligatorio");
        }

        String invitationExamRole = null;
        Boolean invitationExamCanShare = null;
        if (resourceType.equals("exam")) {
            examService.requireExamCanShare(resourceId, owner.getId());
            invitationExamRole = normalizeExamRole(request.examRole());
            invitationExamCanShare = Boolean.TRUE.equals(request.examCanShare());
        } else {
            validateOwnerCanShareResource(owner, resourceType, resourceId);
        }

        String resourceName = resolveResourceName(resourceType, resourceId, request.resourceName());

        Set<Long> recipientIds = normalizeRecipientIds(request.recipientUserIds(), owner.getId());
        if (recipientIds.isEmpty()) {
            throw new BadRequestException("Selecciona al menos un usuario destino");
        }

        List<User> recipients = userRepository.findAllById(recipientIds).stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .toList();
        if (recipients.size() != recipientIds.size()) {
            throw new BadRequestException("Uno o mas usuarios destino no son validos");
        }

        ShareLink shareLink = createShareLink(owner, resourceType, resourceId, request.expiresInHours());
        String invitationStatus =
                resourceType.equals("exam") ? INVITATION_STATUS_PENDING : INVITATION_STATUS_ACCEPTED;
        String resolvedExamRole = invitationExamRole;
        Boolean resolvedExamCanShare = invitationExamCanShare;

        List<ShareNotification> notifications = recipients.stream()
                .map(recipient -> {
                    ShareNotification notification = new ShareNotification();
                    notification.setSenderUser(owner);
                    notification.setRecipientUser(recipient);
                    notification.setShareLink(shareLink);
                    notification.setResourceType(resourceType);
                    notification.setResourceId(resourceId);
                    notification.setResourceName(resourceName);
                    notification.setMessage(buildNotificationMessage(owner, resourceType, resourceName));
                    notification.setInvitationStatus(invitationStatus);
                    notification.setInvitationRespondedAt(null);
                    if (resourceType.equals("exam")) {
                        notification.setExamRole(resolvedExamRole);
                        notification.setExamCanShare(resolvedExamCanShare);
                    } else {
                        notification.setExamRole(null);
                        notification.setExamCanShare(null);
                    }
                    notification.setReadAt(null);
                    notification.setDeletedAt(null);
                    return notification;
                })
                .toList();

        shareNotificationRepository.saveAll(notifications);

        return new ShareLinkDistributeResponse(
                shareLink.getId(),
                resourceType,
                resourceId,
                shareLink.getToken(),
                shareLink.getExpiresAt(),
                notifications.size());
    }

    @Transactional(readOnly = true)
    public List<ShareNotificationResponse> listNotifications(Long userId) {
        requireUser(userId);
        return shareNotificationRepository.findByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional
    public ShareNotificationResponse markNotificationAsRead(Long notificationId, Long userId) {
        requireUser(userId);
        ShareNotification notification = requireNotificationForRecipient(notificationId, userId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = shareNotificationRepository.save(notification);
        }
        return toNotificationResponse(notification);
    }

    @Transactional
    public ShareNotificationResponse acceptNotificationInvitation(Long notificationId, Long userId) {
        User recipient = requireUser(userId);
        ShareNotification notification = requireNotificationForRecipient(notificationId, userId);
        String invitationStatus = normalizeInvitationStatus(notification.getInvitationStatus());
        if (invitationStatus.equals(INVITATION_STATUS_REJECTED)) {
            throw new BadRequestException("Esta invitacion ya fue rechazada.");
        }

        if (normalizeResourceType(notification.getResourceType()).equals("exam")) {
            Exam exam = examRepository.findById(notification.getResourceId())
                    .orElseThrow(() -> new NotFoundException("Examen no encontrado"));
            if (exam.getDeletedAt() != null) {
                throw new NotFoundException("Examen no encontrado");
            }
            examService.upsertExamMembership(
                    exam,
                    recipient,
                    normalizeExamRole(notification.getExamRole()),
                    Boolean.TRUE.equals(notification.getExamCanShare()));
        }

        if (!invitationStatus.equals(INVITATION_STATUS_ACCEPTED)) {
            notification.setInvitationStatus(INVITATION_STATUS_ACCEPTED);
            notification.setInvitationRespondedAt(LocalDateTime.now());
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        notification = shareNotificationRepository.save(notification);
        return toNotificationResponse(notification);
    }

    @Transactional
    public ShareNotificationResponse rejectNotificationInvitation(Long notificationId, Long userId) {
        requireUser(userId);
        ShareNotification notification = requireNotificationForRecipient(notificationId, userId);
        String invitationStatus = normalizeInvitationStatus(notification.getInvitationStatus());
        if (invitationStatus.equals(INVITATION_STATUS_ACCEPTED)) {
            throw new BadRequestException("Esta invitacion ya fue aceptada y no se puede rechazar.");
        }
        if (!invitationStatus.equals(INVITATION_STATUS_REJECTED)) {
            notification.setInvitationStatus(INVITATION_STATUS_REJECTED);
            notification.setInvitationRespondedAt(LocalDateTime.now());
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        notification = shareNotificationRepository.save(notification);
        return toNotificationResponse(notification);
    }

    private ShareNotification requireNotificationForRecipient(Long notificationId, Long userId) {
        return shareNotificationRepository
                .findByIdAndRecipientUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notificacion no encontrada"));
    }

    private void validateShareLinkIsUsable(ShareLink shareLink) {
        if (!Boolean.TRUE.equals(shareLink.getActive())) {
            throw new BadRequestException("Este enlace de comparticion esta inactivo.");
        }

        LocalDateTime expiresAt = shareLink.getExpiresAt();
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            throw new BadRequestException("Este enlace de comparticion ya expiro.");
        }

        Integer maxClaims = shareLink.getMaxClaims();
        Integer claimsCount = shareLink.getClaimsCount();
        if (maxClaims != null && maxClaims > 0 && claimsCount != null && claimsCount >= maxClaims) {
            throw new BadRequestException("Este enlace de comparticion alcanzo su limite de usos.");
        }
    }

    private boolean isShareLinkCurrentlyUsable(ShareLink shareLink) {
        if (!Boolean.TRUE.equals(shareLink.getActive())) {
            return false;
        }

        LocalDateTime expiresAt = shareLink.getExpiresAt();
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }

        Integer maxClaims = shareLink.getMaxClaims();
        Integer claimsCount = shareLink.getClaimsCount();
        return maxClaims == null || maxClaims <= 0 || claimsCount == null || claimsCount < maxClaims;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private int resolveExpiresInHours(Integer value) {
        if (value == null) {
            return DEFAULT_EXPIRES_HOURS;
        }
        int resolved = value.intValue();
        if (resolved <= 0 || resolved > MAX_EXPIRES_HOURS) {
            throw new BadRequestException("expiresInHours debe estar entre 1 y " + MAX_EXPIRES_HOURS);
        }
        return resolved;
    }

    private int resolveRecipientsLimit(Integer value) {
        if (value == null) {
            return DEFAULT_RECIPIENTS_LIMIT;
        }
        int resolved = value.intValue();
        if (resolved <= 0) {
            return DEFAULT_RECIPIENTS_LIMIT;
        }
        if (resolved > MAX_RECIPIENTS_LIMIT) {
            return MAX_RECIPIENTS_LIMIT;
        }
        return resolved;
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeResourceType(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeInvitationStatus(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return INVITATION_STATUS_ACCEPTED;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals(INVITATION_STATUS_PENDING)
                || normalized.equals(INVITATION_STATUS_ACCEPTED)
                || normalized.equals(INVITATION_STATUS_REJECTED)) {
            return normalized;
        }
        return INVITATION_STATUS_ACCEPTED;
    }

    private String normalizeToken(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            throw new BadRequestException("token es obligatorio");
        }
        return normalized;
    }

    private String normalizeDistributionResourceType(String value) {
        String normalized = normalizeResourceType(value);
        if (normalized.equals("exam")
                || normalized.equals("course")
                || normalized.equals("sala")
                || normalized.equals("schedule")) {
            return normalized;
        }
        throw new BadRequestException("resourceType debe ser exam, course, sala o schedule");
    }

    private String normalizeExamRole(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "viewer";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("editor")) {
            return "editor";
        }
        return "viewer";
    }

    private String normalizeExamVisibility(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "private";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("public")) {
            return "public";
        }
        return "private";
    }

    private void validateOwnerCanShareResource(User owner, String resourceType, Long resourceId) {
        if (resourceType.equals("exam")) {
            examService.requireExamCanShare(resourceId, owner.getId());
            return;
        }
        if (resourceType.equals("course")) {
            courseRepository.findByIdAndUserIdAndDeletedAtIsNull(resourceId, owner.getId())
                    .orElseThrow(() -> new NotFoundException("Curso no encontrado"));
            return;
        }
        if (resourceType.equals("sala")) {
            salaService.requireSalaCanShare(resourceId, owner.getId());
            return;
        }
        if (resourceType.equals("schedule")) {
            scheduleService.requireScheduleCanShare(resourceId, owner.getId());
            return;
        }
        throw new BadRequestException("Tipo de recurso de comparticion invalido");
    }

    private String resolveResourceName(String resourceType, Long resourceId, String providedName) {
        String normalizedProvided = trimOrNull(providedName);
        if (normalizedProvided != null) {
            return normalizedProvided;
        }
        if (resourceType.equals("exam")) {
            Exam exam = examRepository.findById(resourceId).orElse(null);
            if (exam != null && exam.getDeletedAt() == null) {
                String examName = trimOrNull(exam.getName());
                if (examName != null) {
                    return examName;
                }
            }
            return "Examen";
        }
        if (resourceType.equals("course")) {
            Course course = courseRepository.findById(resourceId).orElse(null);
            if (course != null && course.getDeletedAt() == null) {
                String courseName = trimOrNull(course.getName());
                if (courseName != null) {
                    return courseName;
                }
            }
            return "Curso";
        }
        if (resourceType.equals("schedule")) {
            try {
                ScheduleProfile scheduleProfile = scheduleService.requireScheduleProfile(resourceId);
                String scheduleName = trimOrNull(scheduleProfile.getName());
                if (scheduleName != null) {
                    return scheduleName;
                }
            } catch (RuntimeException ignored) {
                // Si no existe o no esta disponible, usar etiqueta por defecto.
            }
            return "Horario";
        }
        if (resourceType.equals("sala")) {
            try {
                Sala sala = salaService.requireSala(resourceId);
                String salaName = trimOrNull(sala.getName());
                if (salaName != null) {
                    return salaName;
                }
            } catch (RuntimeException ignored) {
                // Si no existe o no esta disponible, usar etiqueta por defecto.
            }
            return "Sala";
        }
        return "Recurso";
    }

    private Set<Long> normalizeRecipientIds(List<Long> recipientUserIds, Long senderUserId) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long rawId : recipientUserIds) {
            if (rawId == null || rawId <= 0 || rawId.equals(senderUserId)) {
                continue;
            }
            normalized.add(rawId);
        }
        return normalized;
    }

    private String buildNotificationMessage(User owner, String resourceType, String resourceName) {
        String ownerName = trimOrNull(owner.getName());
        if (ownerName == null) {
            ownerName = trimOrNull(owner.getUsername());
        }
        if (ownerName == null) {
            ownerName = "Un usuario";
        }

        String resourceLabel = resourceType.equals("exam")
                ? "examen"
                : resourceType.equals("course")
                        ? "curso"
                        : resourceType.equals("schedule") ? "horario" : "sala";
        return ownerName + " te compartio " + resourceLabel + ": " + resourceName;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ShareNotificationResponse toNotificationResponse(ShareNotification notification) {
        User sender = notification.getSenderUser();
        ShareLink shareLink = notification.getShareLink();
        String senderName = sender == null ? null : trimOrNull(sender.getName());
        String senderUsername = sender == null ? null : trimOrNull(sender.getUsername());
        String resourceType = normalizeResourceType(notification.getResourceType());
        String resourceName = trimOrNull(notification.getResourceName());
        if (resourceName == null) {
            resourceName = resourceType.equals("exam")
                    ? "Examen"
                    : resourceType.equals("course") ? "Curso" : resourceType.equals("schedule") ? "Horario" : "Sala";
        }

        return new ShareNotificationResponse(
                notification.getId(),
                sender == null ? null : sender.getId(),
                senderName == null ? "Usuario" : senderName,
                senderUsername == null ? "" : senderUsername,
                resourceType,
                notification.getResourceId(),
                resourceName,
                trimOrNull(notification.getMessage()),
                shareLink == null ? null : trimOrNull(shareLink.getToken()),
                normalizeInvitationStatus(notification.getInvitationStatus()),
                notification.getInvitationRespondedAt(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }

    private ShareLinkResponse toResponse(ShareLink shareLink) {
        return new ShareLinkResponse(
                shareLink.getId(),
                normalizeResourceType(shareLink.getResourceType()),
                shareLink.getResourceId(),
                shareLink.getToken(),
                shareLink.getExpiresAt(),
                shareLink.getClaimsCount() == null ? 0 : shareLink.getClaimsCount());
    }
}
