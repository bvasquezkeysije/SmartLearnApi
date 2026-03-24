package com.bardales.SmartLearnApi.dto.chat;

import java.util.List;

public record AiModelsResponse(
        String defaultModel,
        List<String> models) {
}

