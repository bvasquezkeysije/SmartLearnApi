package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.support.SupportCallRequestCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportCallRequestResponse;
import com.bardales.SmartLearnApi.dto.support.SupportConversationCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportConversationResponse;
import com.bardales.SmartLearnApi.dto.support.SupportMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.support.SupportMessageResponse;
import com.bardales.SmartLearnApi.dto.support.SupportModuleResponse;
import com.bardales.SmartLearnApi.service.SupportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support")
public class SupportApiController {

    private final SupportService supportService;

    public SupportApiController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/module")
    public SupportModuleResponse getModule(@RequestParam Long userId) {
        return supportService.getModule(userId);
    }

    @PostMapping("/conversations")
    public SupportConversationResponse createConversation(@Valid @RequestBody SupportConversationCreateRequest request) {
        return supportService.createConversation(request);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<SupportMessageResponse> listMessages(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        return supportService.listMessages(conversationId, userId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public SupportMessageResponse sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SupportMessageCreateRequest request) {
        return supportService.sendMessage(conversationId, request);
    }

    @PostMapping("/call-requests")
    public SupportCallRequestResponse createCallRequest(@Valid @RequestBody SupportCallRequestCreateRequest request) {
        return supportService.createCallRequest(request);
    }

    @PostMapping("/conversations/{conversationId}/assign")
    public SupportConversationResponse assignConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        return supportService.assignConversation(conversationId, userId);
    }

    @PostMapping("/conversations/{conversationId}/close")
    public SupportConversationResponse closeConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        return supportService.closeConversation(conversationId, userId);
    }
}

