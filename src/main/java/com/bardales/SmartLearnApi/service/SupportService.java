package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.SupportCallRequest;
import com.bardales.SmartLearnApi.domain.entity.SupportConversation;
import com.bardales.SmartLearnApi.domain.entity.SupportMessage;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.SupportCallRequestRepository;
import com.bardales.SmartLearnApi.domain.repository.SupportConversationRepository;
import com.bardales.SmartLearnApi.domain.repository.SupportMessageRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.support.SupportCallRequestCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportCallRequestResponse;
import com.bardales.SmartLearnApi.dto.support.SupportConversationCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportConversationResponse;
import com.bardales.SmartLearnApi.dto.support.SupportMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportMessageResponse;
import com.bardales.SmartLearnApi.dto.support.SupportModuleResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportService {

    private final SupportConversationRepository supportConversationRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final SupportCallRequestRepository supportCallRequestRepository;
    private final UserRepository userRepository;

    public SupportService(
            SupportConversationRepository supportConversationRepository,
            SupportMessageRepository supportMessageRepository,
            SupportCallRequestRepository supportCallRequestRepository,
            UserRepository userRepository) {
        this.supportConversationRepository = supportConversationRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.supportCallRequestRepository = supportCallRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SupportModuleResponse getModule(Long userId) {
        User requester = requireUser(userId);
        boolean admin = isAdmin(requester);

        List<SupportConversationResponse> conversations = supportConversationRepository
                .findByRequesterUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(requester.getId())
                .stream()
                .map(this::toConversationResponse)
                .toList();

        List<SupportConversationResponse> adminQueue = admin
                ? supportConversationRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc().stream()
                        .map(this::toConversationResponse)
                        .toList()
                : List.of();

        List<SupportCallRequestResponse> callRequests = admin
                ? supportCallRequestRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                        .map(this::toCallRequestResponse)
                        .toList()
                : supportCallRequestRepository.findByRequesterUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(requester.getId()).stream()
                        .map(this::toCallRequestResponse)
                        .toList();

        return new SupportModuleResponse(conversations, adminQueue, callRequests, admin);
    }

    @Transactional
    public SupportConversationResponse createConversation(SupportConversationCreateRequest request) {
        User requester = requireUser(request.userId());
        String subject = trimOrNull(request.subject());
        String initialMessage = trimOrNull(request.initialMessage());
        if (subject == null) {
            throw new BadRequestException("subject es obligatorio");
        }
        if (initialMessage == null) {
            throw new BadRequestException("initialMessage es obligatorio");
        }

        SupportConversation conversation = new SupportConversation();
        conversation.setRequesterUser(requester);
        conversation.setAssignedAdminUser(null);
        conversation.setSubject(subject);
        conversation.setStatus("open");
        conversation.setPriority(normalizePriority(request.priority()));
        conversation.setChannelPreference(normalizeChannelPreference(request.channelPreference()));
        conversation.setTicketType(normalizeTicketType(request.ticketType()));
        conversation.setModuleKey(normalizeModuleKey(request.moduleKey()));
        conversation.setWhatsappNumber(trimOrNull(request.whatsappNumber()));
        conversation.setCallNumber(trimOrNull(request.callNumber()));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation = supportConversationRepository.save(conversation);

        SupportMessage message = new SupportMessage();
        message.setConversation(conversation);
        message.setSenderUser(requester);
        message.setSenderRole("user");
        message.setContent(initialMessage);
        supportMessageRepository.save(message);

        return toConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> listMessages(Long conversationId, Long userId) {
        User requester = requireUser(userId);
        SupportConversation conversation = requireConversation(conversationId);
        assertCanAccessConversation(conversation, requester, isAdmin(requester));
        return supportMessageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public SupportMessageResponse sendMessage(Long conversationId, SupportMessageCreateRequest request) {
        User sender = requireUser(request.userId());
        SupportConversation conversation = requireConversation(conversationId);
        boolean admin = isAdmin(sender);
        assertCanAccessConversation(conversation, sender, admin);

        String messageValue = trimOrNull(request.message());
        if (messageValue == null) {
            throw new BadRequestException("message es obligatorio");
        }

        if ("closed".equalsIgnoreCase(trimOrNull(conversation.getStatus()))) {
            conversation.setStatus(admin ? "in_progress" : "open");
        }

        SupportMessage message = new SupportMessage();
        message.setConversation(conversation);
        message.setSenderUser(sender);
        message.setSenderRole(admin ? "admin" : "user");
        message.setContent(messageValue);
        message = supportMessageRepository.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());
        if (admin && conversation.getAssignedAdminUser() == null) {
            conversation.setAssignedAdminUser(sender);
        }
        supportConversationRepository.save(conversation);

        return toMessageResponse(message);
    }

    @Transactional
    public SupportCallRequestResponse createCallRequest(SupportCallRequestCreateRequest request) {
        User requester = requireUser(request.userId());
        String phoneNumber = trimOrNull(request.phoneNumber());
        String reason = trimOrNull(request.reason());
        if (phoneNumber == null) {
            throw new BadRequestException("phoneNumber es obligatorio");
        }
        if (reason == null) {
            throw new BadRequestException("reason es obligatorio");
        }

        SupportCallRequest callRequest = new SupportCallRequest();
        callRequest.setRequesterUser(requester);
        callRequest.setHandledByAdminUser(null);
        callRequest.setPhoneNumber(phoneNumber);
        callRequest.setPreferredSchedule(trimOrNull(request.preferredSchedule()));
        callRequest.setReason(reason);
        callRequest.setStatus("pending");
        callRequest.setHandledAt(null);
        callRequest = supportCallRequestRepository.save(callRequest);
        return toCallRequestResponse(callRequest);
    }

    @Transactional
    public SupportConversationResponse assignConversation(Long conversationId, Long userId) {
        User adminUser = requireUser(userId);
        assertAdmin(adminUser);
        SupportConversation conversation = requireConversation(conversationId);
        conversation.setAssignedAdminUser(adminUser);
        if (!"closed".equalsIgnoreCase(trimOrNull(conversation.getStatus()))) {
            conversation.setStatus("in_progress");
        }
        conversation = supportConversationRepository.save(conversation);
        return toConversationResponse(conversation);
    }

    @Transactional
    public SupportConversationResponse closeConversation(Long conversationId, Long userId) {
        User adminUser = requireUser(userId);
        assertAdmin(adminUser);
        SupportConversation conversation = requireConversation(conversationId);
        conversation.setStatus("closed");
        if (conversation.getAssignedAdminUser() == null) {
            conversation.setAssignedAdminUser(adminUser);
        }
        conversation = supportConversationRepository.save(conversation);
        return toConversationResponse(conversation);
    }

    private SupportConversation requireConversation(Long conversationId) {
        return supportConversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversacion de soporte no encontrada"));
    }

    private User requireUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BadRequestException("Usuario inactivo");
        }
        return user;
    }

    private void assertCanAccessConversation(SupportConversation conversation, User actor, boolean actorIsAdmin) {
        Long requesterId =
                conversation.getRequesterUser() == null ? null : conversation.getRequesterUser().getId();
        if (actorIsAdmin) {
            return;
        }
        if (requesterId != null && requesterId.equals(actor.getId())) {
            return;
        }
        throw new BadRequestException("Permiso denegado");
    }

    private boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        boolean hasAdminRole = user.hasRole("admin");
        String username = trimOrNull(user.getUsername());
        String email = trimOrNull(user.getEmail());
        boolean adminIdentity = (username != null && username.equalsIgnoreCase("admin"))
                || (email != null && email.equalsIgnoreCase("admin@a21k.com"));
        return hasAdminRole || adminIdentity;
    }

    private void assertAdmin(User user) {
        if (!isAdmin(user)) {
            throw new BadRequestException("Permiso denegado");
        }
    }

    private String normalizePriority(String rawValue) {
        String value = trimOrNull(rawValue);
        if (value == null) {
            return "normal";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("low")
                || normalized.equals("normal")
                || normalized.equals("high")
                || normalized.equals("urgent")) {
            return normalized;
        }
        throw new BadRequestException("priority debe ser low, normal, high o urgent");
    }

    private String normalizeChannelPreference(String rawValue) {
        String value = trimOrNull(rawValue);
        if (value == null) {
            return "chat";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("chat") || normalized.equals("whatsapp") || normalized.equals("call")) {
            return normalized;
        }
        throw new BadRequestException("channelPreference debe ser chat, whatsapp o call");
    }

    private String normalizeTicketType(String rawValue) {
        String value = trimOrNull(rawValue);
        if (value == null) {
            return "support";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("support") || normalized.equals("bug") || normalized.equals("question")) {
            return normalized;
        }
        throw new BadRequestException("ticketType debe ser support, bug o question");
    }

    private String normalizeModuleKey(String rawValue) {
        String value = trimOrNull(rawValue);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.length() > 60) {
            throw new BadRequestException("moduleKey no puede exceder 60 caracteres");
        }
        if (!normalized.matches("[a-z0-9_\\-]+")) {
            throw new BadRequestException("moduleKey solo permite letras, numeros, guion y guion bajo");
        }
        return normalized;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SupportConversationResponse toConversationResponse(SupportConversation conversation) {
        User requester = conversation.getRequesterUser();
        User assignedAdmin = conversation.getAssignedAdminUser();
        String requesterName = requester == null ? "Usuario" : trimOrNull(requester.getName());
        if (requesterName == null) {
            requesterName = "Usuario";
        }
        String requesterUsername = requester == null ? null : trimOrNull(requester.getUsername());
        String assignedAdminName = assignedAdmin == null ? null : trimOrNull(assignedAdmin.getName());
        if (assignedAdminName == null && assignedAdmin != null) {
            assignedAdminName = trimOrNull(assignedAdmin.getUsername());
        }

        return new SupportConversationResponse(
                conversation.getId(),
                requester == null ? null : requester.getId(),
                requesterName,
                requesterUsername,
                assignedAdmin == null ? null : assignedAdmin.getId(),
                assignedAdminName,
                trimOrNull(conversation.getSubject()),
                trimOrNull(conversation.getStatus()),
                trimOrNull(conversation.getPriority()),
                trimOrNull(conversation.getChannelPreference()),
                trimOrNull(conversation.getTicketType()),
                trimOrNull(conversation.getModuleKey()),
                trimOrNull(conversation.getWhatsappNumber()),
                trimOrNull(conversation.getCallNumber()),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt());
    }

    private SupportMessageResponse toMessageResponse(SupportMessage message) {
        User sender = message.getSenderUser();
        String senderName = sender == null ? "Usuario" : trimOrNull(sender.getName());
        if (senderName == null) {
            senderName = "Usuario";
        }
        String senderUsername = sender == null ? null : trimOrNull(sender.getUsername());
        return new SupportMessageResponse(
                message.getId(),
                message.getConversation() == null ? null : message.getConversation().getId(),
                sender == null ? null : sender.getId(),
                senderName,
                senderUsername,
                trimOrNull(message.getSenderRole()),
                trimOrNull(message.getContent()),
                message.getCreatedAt());
    }

    private SupportCallRequestResponse toCallRequestResponse(SupportCallRequest request) {
        User requester = request.getRequesterUser();
        User handledBy = request.getHandledByAdminUser();
        String requesterName = requester == null ? "Usuario" : trimOrNull(requester.getName());
        if (requesterName == null) {
            requesterName = "Usuario";
        }
        String requesterUsername = requester == null ? null : trimOrNull(requester.getUsername());

        return new SupportCallRequestResponse(
                request.getId(),
                requester == null ? null : requester.getId(),
                requesterName,
                requesterUsername,
                trimOrNull(request.getPhoneNumber()),
                trimOrNull(request.getPreferredSchedule()),
                trimOrNull(request.getReason()),
                trimOrNull(request.getStatus()),
                handledBy == null ? null : handledBy.getId(),
                request.getHandledAt(),
                request.getCreatedAt());
    }
}
