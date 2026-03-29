package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Sala;
import com.bardales.SmartLearnApi.domain.entity.SalaMembership;
import com.bardales.SmartLearnApi.domain.entity.SalaMessage;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.SalaMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.SalaMessageRepository;
import com.bardales.SmartLearnApi.domain.repository.SalaRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.sala.SalaCreateRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaJoinByCodeRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaMessageResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaModuleResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaParticipantResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaRoomResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaUpdateRequest;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaService {

    private static final int SALA_NAME_MAX_LENGTH = 180;
    private static final int SALA_CODE_MAX_LENGTH = 60;

    private final UserRepository userRepository;
    private final SalaRepository salaRepository;
    private final SalaMembershipRepository salaMembershipRepository;
    private final SalaMessageRepository salaMessageRepository;

    public SalaService(
            UserRepository userRepository,
            SalaRepository salaRepository,
            SalaMembershipRepository salaMembershipRepository,
            SalaMessageRepository salaMessageRepository) {
        this.userRepository = userRepository;
        this.salaRepository = salaRepository;
        this.salaMembershipRepository = salaMembershipRepository;
        this.salaMessageRepository = salaMessageRepository;
    }

    @Transactional(readOnly = true)
    public SalaModuleResponse getModule(Long userId, Long salaId) {
        User user = requireUser(userId);
        List<AccessContext> contexts = listAccessibleSalas(user);
        List<SalaRoomResponse> salas = contexts.stream()
                .map(context -> toSalaRoomResponse(context, user.getId()))
                .toList();

        Long selectedSalaId = null;
        if (salaId != null && salaId > 0) {
            boolean preferredExists = contexts.stream()
                    .map(AccessContext::sala)
                    .map(Sala::getId)
                    .anyMatch(id -> id != null && id.equals(salaId));
            if (preferredExists) {
                selectedSalaId = salaId;
            }
        }
        if (selectedSalaId == null && !contexts.isEmpty()) {
            selectedSalaId = contexts.get(0).sala().getId();
        }

        return new SalaModuleResponse(salas, selectedSalaId);
    }

    @Transactional
    public SalaRoomResponse createSala(SalaCreateRequest request) {
        User user = requireUser(request.userId());

        String normalizedCode = normalizeSalaCode(request.code());
        if (salaRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(normalizedCode)) {
            throw new BadRequestException("El codigo de sala ya existe. Usa otro codigo unico.");
        }

        Sala sala = new Sala();
        sala.setOwnerUser(user);
        sala.setName(normalizeSalaName(request.name()));
        sala.setCode(normalizedCode);
        sala.setVisibility(normalizeVisibility(request.visibility()));
        sala.setDescription(trimOrNull(request.description()));
        sala.setImageData(trimOrNull(request.imageData()));
        sala.setDeletedAt(null);
        sala = salaRepository.save(sala);

        return toSalaRoomResponse(new AccessContext(sala, "owner", true, true), user.getId());
    }

    @Transactional
    public SalaRoomResponse updateSala(Long salaId, SalaUpdateRequest request) {
        AccessContext access = requireSalaCanEdit(salaId, request.userId());
        Sala sala = access.sala();

        if (request.name() != null) {
            sala.setName(normalizeSalaName(request.name()));
        }

        if (request.code() != null) {
            String normalizedCode = normalizeSalaCode(request.code());
            if (salaRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(normalizedCode, sala.getId())) {
                throw new BadRequestException("El codigo de sala ya existe. Usa otro codigo unico.");
            }
            sala.setCode(normalizedCode);
        }

        if (request.visibility() != null) {
            sala.setVisibility(normalizeVisibility(request.visibility()));
        }

        sala.setDescription(trimOrNull(request.description()));
        sala.setImageData(trimOrNull(request.imageData()));
        sala = salaRepository.save(sala);

        AccessContext refreshed = requireSalaAccess(sala.getId(), request.userId());
        return toSalaRoomResponse(refreshed, request.userId());
    }

    @Transactional
    public void deleteSala(Long salaId, Long userId) {
        User owner = requireUser(userId);
        Sala sala = salaRepository
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(salaId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));

        LocalDateTime now = LocalDateTime.now();
        sala.setDeletedAt(now);
        salaRepository.save(sala);

        List<SalaMembership> memberships = salaMembershipRepository.findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(sala.getId());
        memberships.forEach(membership -> membership.setDeletedAt(now));
        if (!memberships.isEmpty()) {
            salaMembershipRepository.saveAll(memberships);
        }

        List<SalaMessage> messages = salaMessageRepository.findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(sala.getId());
        messages.forEach(message -> message.setDeletedAt(now));
        if (!messages.isEmpty()) {
            salaMessageRepository.saveAll(messages);
        }
    }

    @Transactional
    public SalaRoomResponse joinByCode(SalaJoinByCodeRequest request) {
        User user = requireUser(request.userId());
        String code = normalizeSalaCode(request.code());

        Sala sala = salaRepository
                .findByCodeIgnoreCaseAndDeletedAtIsNull(code)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));

        Long ownerUserId = sala.getOwnerUser() == null ? null : sala.getOwnerUser().getId();
        if (ownerUserId == null || !ownerUserId.equals(user.getId())) {
            upsertSalaMembership(sala, user, "viewer", false);
        }

        AccessContext access = requireSalaAccess(sala.getId(), user.getId());
        return toSalaRoomResponse(access, user.getId());
    }

    @Transactional
    public SalaMessageResponse createMessage(Long salaId, SalaMessageCreateRequest request) {
        AccessContext access = requireSalaAccess(salaId, request.userId());
        User sender = requireUser(request.userId());

        String content = trimOrNull(request.content());
        if (content == null) {
            throw new BadRequestException("content es obligatorio");
        }

        SalaMessage message = new SalaMessage();
        message.setSala(access.sala());
        message.setSenderUser(sender);
        message.setContent(content);
        message.setDeletedAt(null);
        message = salaMessageRepository.save(message);

        return toSalaMessageResponse(message, sender.getId());
    }

    @Transactional(readOnly = true)
    public void requireSalaCanShare(Long salaId, Long userId) {
        AccessContext access = requireSalaAccess(salaId, userId);
        if (!access.canShare()) {
            throw new BadRequestException("No tienes permisos para compartir esta sala.");
        }
    }

    @Transactional(readOnly = true)
    public Sala requireSala(Long salaId) {
        return salaRepository
                .findByIdAndDeletedAtIsNull(salaId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    @Transactional
    public void upsertSalaMembership(Sala sala, User user, String role, boolean canShare) {
        if (sala == null || sala.getId() == null || user == null || user.getId() == null) {
            return;
        }
        Long ownerUserId = sala.getOwnerUser() == null ? null : sala.getOwnerUser().getId();
        if (ownerUserId != null && ownerUserId.equals(user.getId())) {
            return;
        }

        SalaMembership membership = salaMembershipRepository
                .findBySalaIdAndUserId(sala.getId(), user.getId())
                .orElseGet(SalaMembership::new);

        membership.setSala(sala);
        membership.setUser(user);
        membership.setRole(normalizeMembershipRole(role));
        membership.setCanShare(canShare);
        membership.setDeletedAt(null);
        salaMembershipRepository.save(membership);
    }

    private List<AccessContext> listAccessibleSalas(User user) {
        Map<Long, AccessContext> contextsBySalaId = new LinkedHashMap<>();

        List<Sala> ownedSalas = salaRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
        for (Sala sala : ownedSalas) {
            if (sala == null || sala.getId() == null || sala.getDeletedAt() != null) {
                continue;
            }
            contextsBySalaId.put(sala.getId(), new AccessContext(sala, "owner", true, true));
        }

        List<SalaMembership> memberships = salaMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
        for (SalaMembership membership : memberships) {
            if (membership == null || membership.getSala() == null || membership.getSala().getId() == null) {
                continue;
            }
            Sala sala = membership.getSala();
            if (sala.getDeletedAt() != null || contextsBySalaId.containsKey(sala.getId())) {
                continue;
            }
            String role = normalizeMembershipRole(membership.getRole());
            boolean canEdit = !role.equals("viewer");
            boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
            contextsBySalaId.put(sala.getId(), new AccessContext(sala, role, canEdit, canShare));
        }

        return List.copyOf(contextsBySalaId.values());
    }

    private AccessContext requireSalaCanEdit(Long salaId, Long userId) {
        AccessContext access = requireSalaAccess(salaId, userId);
        if (!access.canEdit()) {
            throw new BadRequestException("No tienes permisos para editar esta sala.");
        }
        return access;
    }

    private AccessContext requireSalaAccess(Long salaId, Long userId) {
        if (salaId == null || salaId <= 0) {
            throw new BadRequestException("salaId es obligatorio");
        }

        User user = requireUser(userId);
        Sala sala = requireSala(salaId);

        Long ownerUserId = sala.getOwnerUser() == null ? null : sala.getOwnerUser().getId();
        if (ownerUserId != null && ownerUserId.equals(user.getId())) {
            return new AccessContext(sala, "owner", true, true);
        }

        SalaMembership membership = salaMembershipRepository
                .findBySalaIdAndUserIdAndDeletedAtIsNull(sala.getId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));

        String role = normalizeMembershipRole(membership.getRole());
        boolean canEdit = !role.equals("viewer");
        boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
        return new AccessContext(sala, role, canEdit, canShare);
    }

    private SalaRoomResponse toSalaRoomResponse(AccessContext access, Long currentUserId) {
        Sala sala = access.sala();
        List<SalaParticipantResponse> participants = toSalaParticipants(sala, currentUserId);
        List<SalaMessageResponse> messages = salaMessageRepository
                .findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(sala.getId())
                .stream()
                .map(message -> toSalaMessageResponse(message, currentUserId))
                .toList();

        return new SalaRoomResponse(
                sala.getId(),
                fallbackName(sala.getName(), "Sala"),
                fallbackName(sala.getCode(), "SIN-CODIGO"),
                normalizeVisibility(sala.getVisibility()),
                trimOrNull(sala.getDescription()),
                trimOrNull(sala.getImageData()),
                sala.getOwnerUser() == null ? null : sala.getOwnerUser().getId(),
                access.role(),
                access.canEdit(),
                access.canShare(),
                participants,
                messages,
                sala.getCreatedAt());
    }

    private List<SalaParticipantResponse> toSalaParticipants(Sala sala, Long currentUserId) {
        List<SalaParticipantResponse> participants = new ArrayList<>();
        Set<Long> includedUserIds = new LinkedHashSet<>();

        User owner = sala.getOwnerUser();
        Long ownerUserId = owner == null ? null : owner.getId();
        if (ownerUserId != null && includedUserIds.add(ownerUserId)) {
            participants.add(new SalaParticipantResponse(
                    ownerUserId,
                    ownerUserId,
                    ownerUserId.equals(currentUserId) ? "Tu" : fallbackUserDisplayName(owner),
                    Boolean.FALSE,
                    Boolean.FALSE));
        }

        List<SalaMembership> memberships = salaMembershipRepository.findBySalaIdAndDeletedAtIsNullOrderByCreatedAtAsc(sala.getId());
        for (SalaMembership membership : memberships) {
            if (membership == null || membership.getUser() == null || membership.getUser().getId() == null) {
                continue;
            }
            Long userId = membership.getUser().getId();
            if (!includedUserIds.add(userId)) {
                continue;
            }
            participants.add(new SalaParticipantResponse(
                    userId,
                    userId,
                    userId.equals(currentUserId) ? "Tu" : fallbackUserDisplayName(membership.getUser()),
                    Boolean.FALSE,
                    Boolean.FALSE));
        }

        return participants;
    }

    private SalaMessageResponse toSalaMessageResponse(SalaMessage message, Long currentUserId) {
        User sender = message.getSenderUser();
        Long senderUserId = sender == null ? null : sender.getId();
        boolean isCurrentUser = senderUserId != null && senderUserId.equals(currentUserId);

        return new SalaMessageResponse(
                message.getId(),
                senderUserId,
                isCurrentUser ? "Tu" : fallbackUserDisplayName(sender),
                fallbackName(trimOrNull(message.getContent()), ""),
                isCurrentUser,
                message.getCreatedAt());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String normalizeSalaName(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            throw new BadRequestException("name es obligatorio");
        }
        if (normalized.length() > SALA_NAME_MAX_LENGTH) {
            throw new BadRequestException("name excede el maximo permitido de " + SALA_NAME_MAX_LENGTH + " caracteres");
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SALA ")) {
            return upper;
        }
        return "SALA " + upper;
    }

    private String normalizeSalaCode(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            throw new BadRequestException("code es obligatorio");
        }
        normalized = normalized.toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!normalized.matches("^[A-Z0-9_-]{3," + SALA_CODE_MAX_LENGTH + "}$")) {
            throw new BadRequestException("code debe contener solo letras, numeros, guion o guion bajo");
        }
        return normalized;
    }

    private String normalizeVisibility(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "public";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("public") || normalized.equals("private")) {
            return normalized;
        }
        throw new BadRequestException("visibility debe ser public o private");
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

    private String fallbackUserDisplayName(User user) {
        if (user == null) {
            return "Usuario";
        }
        String name = trimOrNull(user.getName());
        if (name != null) {
            return name;
        }
        String username = trimOrNull(user.getUsername());
        if (username != null) {
            return username;
        }
        return "Usuario";
    }

    private String fallbackName(String value, String fallback) {
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

    private record AccessContext(Sala sala, String role, boolean canEdit, boolean canShare) {}
}
