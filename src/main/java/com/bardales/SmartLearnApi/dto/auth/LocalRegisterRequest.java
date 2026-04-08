package com.bardales.SmartLearnApi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalRegisterRequest(
        @NotBlank(message = "name es obligatorio") String name,
        @NotBlank(message = "username es obligatorio") @Size(min = 3, max = 30, message = "username debe tener entre 3 y 30 caracteres") String username,
        @NotBlank(message = "email es obligatorio") @Email(message = "email invalido") String email,
        @NotBlank(message = "password es obligatorio") @Size(min = 8, message = "password debe tener al menos 8 caracteres") String password,
        @NotBlank(message = "confirmPassword es obligatorio") @Size(min = 8, message = "confirmPassword debe tener al menos 8 caracteres") String confirmPassword) {
}
