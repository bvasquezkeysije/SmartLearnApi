package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.task.TaskRequest;
import com.bardales.SmartLearnApi.dto.task.TaskResponse;
import com.bardales.SmartLearnApi.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskApiController {

    private final TaskService taskService;

    public TaskApiController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam Long requesterUserId, @RequestParam(required = false) Long projectId) {
        return taskService.listTasks(requesterUserId, projectId);
    }

    @PostMapping
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(taskId, request);
    }

    @PatchMapping("/{taskId}/toggle-status")
    public TaskResponse toggleStatus(@PathVariable Long taskId, @RequestParam Long requesterUserId) {
        return taskService.toggleTaskStatus(taskId, requesterUserId);
    }

    @DeleteMapping("/{taskId}")
    public void delete(@PathVariable Long taskId, @RequestParam Long requesterUserId) {
        taskService.deleteTask(taskId, requesterUserId);
    }
}
