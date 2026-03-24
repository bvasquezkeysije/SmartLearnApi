package com.bardales.SmartLearnApi.dto.support;

import java.util.List;

public record SupportModuleResponse(
        List<SupportConversationResponse> conversations,
        List<SupportConversationResponse> adminQueue,
        List<SupportCallRequestResponse> callRequests,
        boolean adminView) {
}

