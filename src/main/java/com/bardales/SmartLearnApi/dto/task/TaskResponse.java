package com.bardales.SmartLearnApi.dto.task;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String status,
        String priority,
        boolean deleted) {
}