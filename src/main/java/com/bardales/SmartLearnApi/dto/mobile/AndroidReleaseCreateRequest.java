package com.bardales.SmartLearnApi.dto.mobile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AndroidReleaseCreateRequest(
        @NotBlank(message = "versionName es obligatorio") @Size(max = 64, message = "versionName maximo 64 caracteres") String versionName,
        @Min(value = 1, message = "versionCode debe ser mayor a 0") Integer versionCode,
        @NotBlank(message = "apkUrl es obligatorio") @Size(max = 2000, message = "apkUrl maximo 2000 caracteres") String apkUrl,
        @Size(max = 128, message = "checksumSha256 maximo 128 caracteres") String checksumSha256,
        String releaseNotes,
        Boolean isActive) {
}
