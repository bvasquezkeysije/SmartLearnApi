package com.bardales.SmartLearnApi.dto.role;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(
        @NotBlank(message = "roleName es obligatorio") String roleName) {
}
