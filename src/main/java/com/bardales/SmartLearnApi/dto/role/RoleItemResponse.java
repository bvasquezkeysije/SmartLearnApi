package com.bardales.SmartLearnApi.dto.role;

import java.util.List;

public record RoleItemResponse(
        Long id,
        String name,
        List<String> permissions) {
}
