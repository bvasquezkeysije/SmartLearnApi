package com.bardales.SmartLearnApi.dto.project;

public record ProjectResponse(
        Long id,
        Long userId,
        String name,
        String description,
        boolean deleted) {
}