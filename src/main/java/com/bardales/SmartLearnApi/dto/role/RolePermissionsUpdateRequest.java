package com.bardales.SmartLearnApi.dto.role;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RolePermissionsUpdateRequest(
        @NotEmpty(message = "permissions no puede estar vacio") List<String> permissions) {
}
