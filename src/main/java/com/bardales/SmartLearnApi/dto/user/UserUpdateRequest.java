package com.bardales.SmartLearnApi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "firstName es obligatorio") String firstName,
        @NotBlank(message = "lastName es obligatorio") String lastName,
        @NotBlank(message = "username es obligatorio") String username,
        @Email(message = "email invalido") @NotBlank(message = "email es obligatorio") String email,
        @Size(min = 3, message = "password minimo 3") String password,
        @NotBlank(message = "role es obligatorio") String role) {
}