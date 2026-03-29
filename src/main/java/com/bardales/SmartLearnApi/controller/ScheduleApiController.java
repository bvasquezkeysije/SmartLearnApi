package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.schedule.ScheduleActivityResponse;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleActivitySaveRequest;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleModuleResponse;
import com.bardales.SmartLearnApi.service.ScheduleService;
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
@RequestMapping("/api/v1/schedules")
public class ScheduleApiController {

    private final ScheduleService scheduleService;

    public ScheduleApiController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ScheduleModuleResponse getModule(@RequestParam Long userId, @RequestParam(required = false) Long scheduleId) {
        return scheduleService.getModule(userId, scheduleId);
    }

    @PostMapping("/{scheduleId}/activities")
    public ScheduleActivityResponse createActivity(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleActivitySaveRequest request) {
        return scheduleService.createActivity(scheduleId, request);
    }

    @PatchMapping("/{scheduleId}/activities/{activityId}")
    public ScheduleActivityResponse updateActivity(
            @PathVariable Long scheduleId,
            @PathVariable Long activityId,
            @Valid @RequestBody ScheduleActivitySaveRequest request) {
        return scheduleService.updateActivity(scheduleId, activityId, request);
    }

    @DeleteMapping("/{scheduleId}/activities/{activityId}")
    public void deleteActivity(
            @PathVariable Long scheduleId,
            @PathVariable Long activityId,
            @RequestParam Long userId) {
        scheduleService.deleteActivity(scheduleId, activityId, userId);
    }
}
