package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.AiChat;
import com.bardales.SmartLearnApi.domain.entity.AiChatMessage;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.AiChatMessageRepository;
import com.bardales.SmartLearnApi.domain.repository.AiChatRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.chat.ChatCreateRequest;
import com.bardales.SmartLearnApi.dto.chat.ChatDetailResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.chat.ChatMessageResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatSummaryResponse;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatService {

    private static final int MAX_CHAT_CONTENT_LENGTH = 250;

    private final AiChatRepository chatRepository;
    private final AiChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public AiChatService(
            AiChatRepository chatRepository,
            AiChatMessageRepository messageRepository,
            UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatSummaryResponse> listChats(Long userId) {
        requireUser(userId);

        return chatRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(chat -> new ChatSummaryResponse(
                        chat.getId(),
                        chat.getName(),
                        messageRepository.countByChatId(chat.getId()),
                        chat.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ChatDetailResponse createChat(ChatCreateRequest request) {
        User user = requireUser(request.userId());
        String firstMessage = request.firstMessage() == null ? "" : request.firstMessage().trim();
        String chatName;
        if (firstMessage.isBlank()) {
            chatName = "Nuevo chat";
        } else {
            chatName = firstMessage.length() > 90 ? firstMessage.substring(0, 90) + "..." : firstMessage;
        }

        AiChat chat = new AiChat();
        chat.setUser(user);
        chat.setName(chatName);
        chat = chatRepository.save(chat);

        if (!firstMessage.isBlank()) {
            String userContent = buildUserContent(firstMessage, request.attachmentName());
            createMessage(chat, "user", userContent);
            createMessage(chat, "assistant", buildAssistantReply(request.attachmentName()));
        }

        return getChat(request.userId(), chat.getId());
    }

    @Transactional(readOnly = true)
    public ChatDetailResponse getChat(Long userId, Long chatId) {
        AiChat chat = chatRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new NotFoundException("Chat no encontrado"));

        List<ChatMessageResponse> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(this::toMessageResponse)
                .toList();

        return new ChatDetailResponse(chat.getId(), chat.getName(), messages);
    }

    @Transactional
    public ChatDetailResponse addMessage(Long chatId, ChatMessageCreateRequest request) {
        AiChat chat = chatRepository.findByIdAndUserId(chatId, request.userId())
                .orElseThrow(() -> new NotFoundException("Chat no encontrado"));

        String content = buildUserContent(request.content().trim(), request.attachmentName());
        createMessage(chat, "user", content);
        createMessage(chat, "assistant", buildAssistantReply(request.attachmentName()));

        return getChat(request.userId(), chatId);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String buildUserContent(String content, String attachmentName) {
        if (attachmentName == null || attachmentName.isBlank()) {
            return content;
        }
        return content + "\n\n[Archivo adjunto: " + attachmentName.trim() + "]";
    }

    private String buildAssistantReply(String attachmentName) {
        if (attachmentName == null || attachmentName.isBlank()) {
            return "Entendido. Continuemos con el siguiente paso de tu practica.";
        }
        return "Perfecto. Ya recibi tus archivos. Ahora dime el nombre del examen y cuantas preguntas quieres (10-100).";
    }

    private void createMessage(AiChat chat, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setChat(chat);
        message.setRole(role);
        message.setContent(normalizeContent(content));
        messageRepository.save(message);
    }

    private String normalizeContent(String content) {
        String safe = content == null ? "" : content.trim();
        if (safe.length() <= MAX_CHAT_CONTENT_LENGTH) {
            return safe;
        }
        return safe.substring(0, MAX_CHAT_CONTENT_LENGTH - 3).trim() + "...";
    }

    private ChatMessageResponse toMessageResponse(AiChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}
