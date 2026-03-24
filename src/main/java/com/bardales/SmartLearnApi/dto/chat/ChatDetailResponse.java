package com.bardales.SmartLearnApi.dto.chat;

import java.util.List;

public record ChatDetailResponse(
        Long id,
        String name,
        List<ChatMessageResponse> messages) {
}