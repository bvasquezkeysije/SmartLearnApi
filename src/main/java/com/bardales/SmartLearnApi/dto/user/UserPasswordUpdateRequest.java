package com.bardales.SmartLearnApi.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
        String currentPassword,
        @NotBlank(message = "newPassword es obligatorio")
        @Size(min = 3, message = "newPassword minimo 3")
        String newPassword) {
}
