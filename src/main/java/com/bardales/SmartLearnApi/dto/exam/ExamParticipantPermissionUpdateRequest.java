package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamParticipantPermissionUpdateRequest(
        @NotNull(message = "requesterUserId es obligatorio") Long requesterUserId,
        @NotBlank(message = "role es obligatorio") String role,
        Boolean canShare,
        Boolean canStartGroup,
        Boolean canRenameExam) {}
