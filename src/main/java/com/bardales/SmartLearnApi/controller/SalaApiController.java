package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.sala.SalaCreateRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaJoinByCodeRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaMessageCreateRequest;
import com.bardales.SmartLearnApi.dto.sala.SalaMessageResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaModuleResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaRoomResponse;
import com.bardales.SmartLearnApi.dto.sala.SalaUpdateRequest;
import com.bardales.SmartLearnApi.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salas")
public class SalaApiController {

    private final SalaService salaService;

    public SalaApiController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public SalaModuleResponse getModule(@RequestParam Long userId, @RequestParam(required = false) Long salaId) {
        return salaService.getModule(userId, salaId);
    }

    @PostMapping
    public SalaRoomResponse createSala(@Valid @RequestBody SalaCreateRequest request) {
        return salaService.createSala(request);
    }

    @PatchMapping("/{salaId}")
    public SalaRoomResponse updateSala(@PathVariable Long salaId, @Valid @RequestBody SalaUpdateRequest request) {
        return salaService.updateSala(salaId, request);
    }

    @DeleteMapping("/{salaId}")
    public void deleteSala(@PathVariable Long salaId, @RequestParam Long userId) {
        salaService.deleteSala(salaId, userId);
    }

    @PostMapping("/join-by-code")
    public SalaRoomResponse joinByCode(@Valid @RequestBody SalaJoinByCodeRequest request) {
        return salaService.joinByCode(request);
    }

    @PostMapping("/{salaId}/messages")
    public SalaMessageResponse createMessage(
            @PathVariable Long salaId,
            @Valid @RequestBody SalaMessageCreateRequest request) {
        return salaService.createMessage(salaId, request);
    }
}
