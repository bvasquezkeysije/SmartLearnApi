package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.chat.AiModelsResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatCreateRequest;
import com.bardales.SmartLearnApi.dto.chat.ChatDetailResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatGenerateExamResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.chat.ChatSummaryResponse;
import com.bardales.SmartLearnApi.service.AiChatService;
import com.bardales.SmartLearnApi.service.AiExamGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ia")
public class AiApiController {

    private final AiChatService aiChatService;
    private final AiExamGenerationService aiExamGenerationService;

    public AiApiController(AiChatService aiChatService, AiExamGenerationService aiExamGenerationService) {
        this.aiChatService = aiChatService;
        this.aiExamGenerationService = aiExamGenerationService;
    }

    @GetMapping("/chats")
    public List<ChatSummaryResponse> listChats(@RequestParam Long userId) {
        return aiChatService.listChats(userId);
    }

    @PostMapping("/chats")
    public ChatDetailResponse createChat(@Valid @RequestBody ChatCreateRequest request) {
        return aiChatService.createChat(request);
    }

    @GetMapping("/chats/{chatId}")
    public ChatDetailResponse getChat(@PathVariable Long chatId, @RequestParam Long userId) {
        return aiChatService.getChat(userId, chatId);
    }

    @PostMapping("/chats/{chatId}/messages")
    public ChatDetailResponse sendMessage(@PathVariable Long chatId, @Valid @RequestBody ChatMessageCreateRequest request) {
        return aiChatService.addMessage(chatId, request);
    }

    @GetMapping("/models")
    public AiModelsResponse listModels(@RequestParam Long userId) {
        return aiExamGenerationService.listModels(userId);
    }

    @PostMapping(value = "/chats/{chatId}/generate-exam", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatGenerateExamResponse generateExamFromPdf(
            @PathVariable Long chatId,
            @RequestParam Long userId,
            @RequestParam(required = false) String examName,
            @RequestParam(required = false) String instructions,
            @RequestParam(required = false, defaultValue = "10") Integer questionsCount,
            @RequestParam(required = false) String model,
            @RequestParam("files") MultipartFile[] files) {
        return aiExamGenerationService.generateExamFromPdf(
                chatId,
                userId,
                examName,
                instructions,
                questionsCount,
                model,
                files);
    }
}
