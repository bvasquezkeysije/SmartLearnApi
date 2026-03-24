package com.bardales.SmartLearnApi.dto.role;

import java.util.List;

public record RoleManagementResponse(
        List<RoleItemResponse> roles,
        List<String> availablePermissions) {
}
