package com.bardales.SmartLearnApi.dto.sala;

import java.util.List;

public record SalaModuleResponse(
        List<SalaRoomResponse> salas,
        Long selectedSalaId) {}
