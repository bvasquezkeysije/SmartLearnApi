package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.ScheduleActivity;
import com.bardales.SmartLearnApi.domain.entity.ScheduleMembership;
import com.bardales.SmartLearnApi.domain.entity.ScheduleProfile;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ScheduleActivityRepository;
import com.bardales.SmartLearnApi.domain.repository.ScheduleMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ScheduleProfileRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleActivityResponse;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleActivitySaveRequest;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleModuleResponse;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleProfileOptionResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private static final Pattern TIME_24H_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final Set<String> ALLOWED_DAYS = Set.of(
            "all",
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday");
        private static final Set<String> ALLOWED_COLORS = Set.of(
            "blue",
            "emerald",
            "amber",
            "violet",
            "rose",
            "cyan",
            "indigo",
            "teal",
            "orange",
            "lime",
            "fuchsia",
            "slate",
            "red",
            "pink");

    private final UserRepository userRepository;
    private final ScheduleProfileRepository scheduleProfileRepository;
    private final ScheduleMembershipRepository scheduleMembershipRepository;
    private final ScheduleActivityRepository scheduleActivityRepository;

    public ScheduleService(
            UserRepository userRepository,
            ScheduleProfileRepository scheduleProfileRepository,
            ScheduleMembershipRepository scheduleMembershipRepository,
            ScheduleActivityRepository scheduleActivityRepository) {
        this.userRepository = userRepository;
        this.scheduleProfileRepository = scheduleProfileRepository;
        this.scheduleMembershipRepository = scheduleMembershipRepository;
        this.scheduleActivityRepository = scheduleActivityRepository;
    }

    @Transactional
    public ScheduleModuleResponse getModule(Long userId, Long scheduleId) {
        User user = requireUser(userId);
        ensurePersonalBaseProfile(user);
        AccessContext access = resolveAccessContext(user, scheduleId);
        List<ScheduleActivityResponse> activities = scheduleActivityRepository
                .findByScheduleProfileIdAndDeletedAtIsNullOrderByCreatedAtAsc(access.profile().getId())
                .stream()
                .sorted(Comparator.comparingInt((ScheduleActivity activity) -> dayOrder(activity.getDayKey()))
                        .thenComparingInt(activity -> timeToMinutes(activity.getStartTime()))
                        .thenComparing(activity -> normalizeText(activity.getTitle(), "")))
                .map(this::toActivityResponse)
                .toList();

        ScheduleProfile profile = access.profile();
        Long ownerUserId = profile.getOwnerUser() == null ? null : profile.getOwnerUser().getId();
        List<ScheduleProfileOptionResponse> availableProfiles = listAvailableProfiles(user);
        long ownProfiles = availableProfiles.stream()
            .filter(option -> option != null && option.ownerUserId() != null && option.ownerUserId().equals(user.getId()))
            .count();
        long sharedProfiles = Math.max(0, availableProfiles.size() - ownProfiles);
        log.info(
            "SCHEDULES_MODULE userId={} requestedScheduleId={} selectedProfileId={} accessRole={} ownProfiles={} sharedProfiles={}",
            user.getId(),
            scheduleId,
            profile.getId(),
            access.role(),
            ownProfiles,
            sharedProfiles);
        return new ScheduleModuleResponse(
                profile.getId(),
                fallbackName(profile.getName(), "Mi horario"),
                trimOrNull(profile.getDescription()),
                ownerUserId,
                access.role(),
                access.canEdit(),
                access.canShare(),
                availableProfiles,
                trimOrNull(profile.getReferenceImageData()),
                trimOrNull(profile.getReferenceImageName()),
                activities,
                profile.getCreatedAt());
    }

    @Transactional
    public ScheduleActivityResponse createActivity(Long scheduleId, ScheduleActivitySaveRequest request) {
        AccessContext access = requireScheduleCanEdit(scheduleId, request.userId());

        ScheduleActivity activity = new ScheduleActivity();
        activity.setScheduleProfile(access.profile());
        applyActivityRequest(activity, request);
        activity.setDeletedAt(null);
        activity = scheduleActivityRepository.save(activity);

        return toActivityResponse(activity);
    }

    @Transactional
    public ScheduleActivityResponse updateActivity(Long scheduleId, Long activityId, ScheduleActivitySaveRequest request) {
        AccessContext access = requireScheduleCanEdit(scheduleId, request.userId());

        ScheduleActivity activity = scheduleActivityRepository
                .findByIdAndScheduleProfileIdAndDeletedAtIsNull(activityId, access.profile().getId())
                .orElseThrow(() -> new NotFoundException("Actividad de horario no encontrada"));

        applyActivityRequest(activity, request);
        activity = scheduleActivityRepository.save(activity);

        return toActivityResponse(activity);
    }

    @Transactional
    public void deleteActivity(Long scheduleId, Long activityId, Long userId) {
        AccessContext access = requireScheduleCanEdit(scheduleId, userId);

        ScheduleActivity activity = scheduleActivityRepository
                .findByIdAndScheduleProfileIdAndDeletedAtIsNull(activityId, access.profile().getId())
                .orElseThrow(() -> new NotFoundException("Actividad de horario no encontrada"));

        activity.setDeletedAt(LocalDateTime.now());
        scheduleActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public void requireScheduleCanShare(Long scheduleId, Long userId) {
        AccessContext access = requireScheduleAccess(scheduleId, userId);
        if (!access.canShare()) {
            throw new BadRequestException("No tienes permisos para compartir este horario.");
        }
    }

    @Transactional
    public void upsertScheduleMembership(ScheduleProfile scheduleProfile, User user, String role, boolean canShare) {
        if (scheduleProfile == null || scheduleProfile.getId() == null || user == null || user.getId() == null) {
            return;
        }
        Long ownerUserId = scheduleProfile.getOwnerUser() == null ? null : scheduleProfile.getOwnerUser().getId();
        if (ownerUserId != null && ownerUserId.equals(user.getId())) {
            return;
        }

        ScheduleMembership membership = scheduleMembershipRepository
                .findByScheduleProfileIdAndUserId(scheduleProfile.getId(), user.getId())
                .orElseGet(ScheduleMembership::new);

        membership.setScheduleProfile(scheduleProfile);
        membership.setUser(user);
        membership.setRole(normalizeMembershipRole(role));
        membership.setCanShare(canShare);
        membership.setDeletedAt(null);
        scheduleMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public ScheduleProfile requireScheduleProfile(Long scheduleId) {
        return scheduleProfileRepository
                .findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new NotFoundException("Horario no encontrado"));
    }

    private AccessContext requireScheduleCanEdit(Long scheduleId, Long userId) {
        AccessContext access = requireScheduleAccess(scheduleId, userId);
        if (!access.canEdit()) {
            throw new BadRequestException("No tienes permisos para editar este horario.");
        }
        return access;
    }

    private AccessContext resolveAccessContext(User user, Long preferredScheduleId) {
        if (preferredScheduleId != null && preferredScheduleId > 0) {
            return requireScheduleAccess(preferredScheduleId, user.getId());
        }

        ScheduleProfile personalBaseProfile = ensurePersonalBaseProfile(user);
        if (personalBaseProfile != null && personalBaseProfile.getId() != null) {
            return new AccessContext(personalBaseProfile, "owner", true, true);
        }

        List<ScheduleMembership> memberships = scheduleMembershipRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
        for (ScheduleMembership membership : memberships) {
            if (membership == null) {
                continue;
            }
            ScheduleProfile profile = membership.getScheduleProfile();
            if (profile == null || profile.getId() == null || profile.getDeletedAt() != null) {
                continue;
            }
            String role = normalizeMembershipRole(membership.getRole());
            boolean canEdit = !role.equals("viewer");
            boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
            return new AccessContext(profile, role, canEdit, canShare);
        }

        throw new NotFoundException("No se pudo resolver un horario para el usuario.");
    }

    private List<ScheduleProfileOptionResponse> listAvailableProfiles(User user) {
        Map<Long, ScheduleProfileOptionResponse> profilesById = new LinkedHashMap<>();

        List<ScheduleProfile> ownedProfiles = listOwnedProfiles(user);
        if (ownedProfiles.isEmpty()) {
            ScheduleProfile personalBaseProfile = ensurePersonalBaseProfile(user);
            ownedProfiles = personalBaseProfile == null ? List.of() : List.of(personalBaseProfile);
        }

        for (ScheduleProfile profile : ownedProfiles) {
            if (profile == null || profile.getId() == null || profile.getDeletedAt() != null) {
                continue;
            }
            String ownerName = profile.getOwnerUser() == null
                    ? trimOrNull(user.getName())
                    : fallbackName(trimOrNull(profile.getOwnerUser().getName()), trimOrNull(profile.getOwnerUser().getUsername()));
            profilesById.put(profile.getId(), toProfileOptionResponse(profile, "owner", true, true, ownerName));
        }

        List<ScheduleMembership> memberships = listSharedMemberships(user);
        for (ScheduleMembership membership : memberships) {
            if (membership == null) {
                continue;
            }
            ScheduleProfile profile = membership.getScheduleProfile();
            if (profile == null || profile.getId() == null || profile.getDeletedAt() != null) {
                continue;
            }
            if (profilesById.containsKey(profile.getId())) {
                continue;
            }
            String role = normalizeMembershipRole(membership.getRole());
            boolean canEdit = !role.equals("viewer");
            boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
            String ownerName = profile.getOwnerUser() == null
                    ? null
                    : fallbackName(trimOrNull(profile.getOwnerUser().getName()), trimOrNull(profile.getOwnerUser().getUsername()));
            profilesById.put(profile.getId(), toProfileOptionResponse(profile, role, canEdit, canShare, ownerName));
        }

        if (profilesById.isEmpty()) {
            ScheduleProfile defaultProfile = createDefaultProfile(user);
            String ownerName = fallbackName(trimOrNull(user.getName()), trimOrNull(user.getUsername()));
            profilesById.put(defaultProfile.getId(), toProfileOptionResponse(defaultProfile, "owner", true, true, ownerName));
        }

        return List.copyOf(profilesById.values());
    }

    private List<ScheduleProfile> listOwnedProfiles(User user) {
        return scheduleProfileRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
    }

    private List<ScheduleMembership> listSharedMemberships(User user) {
        return scheduleMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
    }

    private ScheduleProfile ensurePersonalBaseProfile(User user) {
        List<ScheduleProfile> ownedProfiles = listOwnedProfiles(user);
        for (ScheduleProfile profile : ownedProfiles) {
            if (profile != null && profile.getId() != null && profile.getDeletedAt() == null) {
                return profile;
            }
        }
        return createDefaultProfile(user);
    }

    private ScheduleProfileOptionResponse toProfileOptionResponse(
            ScheduleProfile profile,
            String accessRole,
            boolean canEdit,
            boolean canShare,
            String ownerName) {
        Long ownerUserId = profile.getOwnerUser() == null ? null : profile.getOwnerUser().getId();
        return new ScheduleProfileOptionResponse(
                profile.getId(),
                fallbackName(profile.getName(), "Mi horario"),
                ownerUserId,
                trimOrNull(ownerName),
                accessRole,
                canEdit,
                canShare,
                profile.getCreatedAt());
    }

    private AccessContext requireScheduleAccess(Long scheduleId, Long userId) {
        if (scheduleId == null || scheduleId <= 0) {
            throw new BadRequestException("scheduleId es obligatorio");
        }

        User user = requireUser(userId);
        ScheduleProfile profile = requireScheduleProfile(scheduleId);

        Long ownerUserId = profile.getOwnerUser() == null ? null : profile.getOwnerUser().getId();
        if (ownerUserId != null && ownerUserId.equals(user.getId())) {
            return new AccessContext(profile, "owner", true, true);
        }

        ScheduleMembership membership = scheduleMembershipRepository
                .findByScheduleProfileIdAndUserIdAndDeletedAtIsNull(profile.getId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Horario no encontrado"));

        String role = normalizeMembershipRole(membership.getRole());
        boolean canEdit = !role.equals("viewer");
        boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
        return new AccessContext(profile, role, canEdit, canShare);
    }

    private ScheduleProfile createDefaultProfile(User user) {
        ScheduleProfile profile = new ScheduleProfile();
        profile.setOwnerUser(user);
        String ownerName = trimOrNull(user.getName());
        if (ownerName == null) {
            ownerName = trimOrNull(user.getUsername());
        }
        if (ownerName == null) {
            ownerName = "Usuario";
        }
        profile.setName("Horario de " + ownerName);
        profile.setDescription(null);
        profile.setVisibility("private");
        profile.setReferenceImageData(null);
        profile.setReferenceImageName(null);
        profile.setDeletedAt(null);
        ScheduleProfile saved = scheduleProfileRepository.save(profile);
        log.info(
            "SCHEDULES_DEFAULT_PROFILE_CREATED requesterUserId={} ownerUserId={} profileId={} profileName={}",
            user.getId(),
            saved.getOwnerUser() == null ? null : saved.getOwnerUser().getId(),
            saved.getId(),
            saved.getName());
        return saved;
    }

    private void applyActivityRequest(ScheduleActivity activity, ScheduleActivitySaveRequest request) {
        String title = trimOrNull(request.title());
        if (title == null) {
            throw new BadRequestException("title es obligatorio");
        }

        String day = normalizeDay(request.day());
        String startTime = normalizeTime(request.startTime(), "startTime");
        String endTime = normalizeTime(request.endTime(), "endTime");
        if (timeToMinutes(endTime) <= timeToMinutes(startTime)) {
            throw new BadRequestException("endTime debe ser mayor que startTime");
        }

        activity.setTitle(title);
        activity.setDescription(trimOrNull(request.description()));
        activity.setDayKey(day);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setLocation(trimOrNull(request.location()));
        activity.setColorKey(normalizeColor(request.color()));
        activity.setSortOrder(request.sortOrder());
    }

    private ScheduleActivityResponse toActivityResponse(ScheduleActivity activity) {
        return new ScheduleActivityResponse(
                activity.getId(),
                fallbackName(activity.getTitle(), "Actividad"),
                trimOrNull(activity.getDescription()),
                normalizeDay(activity.getDayKey()),
                normalizeTime(activity.getStartTime(), "startTime"),
                normalizeTime(activity.getEndTime(), "endTime"),
                trimOrNull(activity.getLocation()),
                normalizeColor(activity.getColorKey()),
                activity.getSortOrder(),
                activity.getCreatedAt());
    }

    private int dayOrder(String day) {
        String normalized = normalizeDay(day);
        if (normalized.equals("all")) {
            return 0;
        }
        if (normalized.equals("monday")) {
            return 1;
        }
        if (normalized.equals("tuesday")) {
            return 2;
        }
        if (normalized.equals("wednesday")) {
            return 3;
        }
        if (normalized.equals("thursday")) {
            return 4;
        }
        if (normalized.equals("friday")) {
            return 5;
        }
        if (normalized.equals("saturday")) {
            return 6;
        }
        return 7;
    }

    private int timeToMinutes(String value) {
        String safe = normalizeTime(value, "time");
        int hours = Integer.parseInt(safe.substring(0, 2));
        int minutes = Integer.parseInt(safe.substring(3, 5));
        return hours * 60 + minutes;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String normalizeMembershipRole(String value) {
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

    private String normalizeDay(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            throw new BadRequestException("day es obligatorio");
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_DAYS.contains(normalized)) {
            throw new BadRequestException("day debe ser all, monday, tuesday, wednesday, thursday, friday, saturday o sunday");
        }
        return normalized;
    }

    private String normalizeTime(String value, String fieldName) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " es obligatorio");
        }
        if (!TIME_24H_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(fieldName + " debe estar en formato HH:mm");
        }
        return normalized;
    }

    private String normalizeColor(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "blue";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_COLORS.contains(normalized)) {
            return "blue";
        }
        return normalized;
    }

    private String fallbackName(String value, String fallback) {
        String normalized = trimOrNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeText(String value, String fallback) {
        String normalized = trimOrNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record AccessContext(ScheduleProfile profile, String role, boolean canEdit, boolean canShare) {}
}




