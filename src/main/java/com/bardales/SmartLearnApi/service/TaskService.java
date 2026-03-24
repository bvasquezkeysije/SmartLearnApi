package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Project;
import com.bardales.SmartLearnApi.domain.entity.Task;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ProjectRepository;
import com.bardales.SmartLearnApi.domain.repository.TaskRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.task.TaskRequest;
import com.bardales.SmartLearnApi.dto.task.TaskResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Set<String> VALID_STATUS = Set.of("pending", "in_progress", "completed");
    private static final Set<String> VALID_PRIORITY = Set.of("low", "medium", "high");

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(Long requesterUserId, Long projectId) {
        User requester = requireUser(requesterUserId);

        List<Task> tasks;
        if (projectId != null) {
            Project project = requireProject(projectId);
            assertProjectAccess(requester, project);
            tasks = taskRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);
        } else if (requester.hasRole("admin")) {
            tasks = taskRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        } else {
            tasks = taskRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                    .filter(task -> task.getProject().getUser().getId().equals(requester.getId()))
                    .toList();
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User requester = requireUser(request.requesterUserId());
        Project project = requireProject(request.projectId());
        assertProjectAccess(requester, project);

        String status = normalizeStatus(request.status());
        String priority = normalizePriority(request.priority());

        Task task = new Task();
        task.setProject(project);
        task.setTitle(request.title().trim());
        task.setStatus(status);
        task.setPriority(priority);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        User requester = requireUser(request.requesterUserId());
        Task task = requireTask(taskId);
        Project project = requireProject(request.projectId());
        assertProjectAccess(requester, project);

        task.setProject(project);
        task.setTitle(request.title().trim());
        task.setStatus(normalizeStatus(request.status()));
        task.setPriority(normalizePriority(request.priority()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse toggleTaskStatus(Long taskId, Long requesterUserId) {
        User requester = requireUser(requesterUserId);
        Task task = requireTask(taskId);
        assertProjectAccess(requester, task.getProject());

        if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
            task.setStatus(Task.STATUS_PENDING);
        } else {
            task.setStatus(Task.STATUS_COMPLETED);
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId, Long requesterUserId) {
        User requester = requireUser(requesterUserId);
        Task task = requireTask(taskId);
        assertProjectAccess(requester, task.getProject());

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase();
        if (!VALID_STATUS.contains(value)) {
            throw new BadRequestException("Status invalido");
        }
        return value;
    }

    private String normalizePriority(String priority) {
        String value = priority == null ? "" : priority.trim().toLowerCase();
        if (!VALID_PRIORITY.contains(value)) {
            throw new BadRequestException("Priority invalida");
        }
        return value;
    }

    private void assertProjectAccess(User requester, Project project) {
        if (!requester.hasRole("admin") && !project.getUser().getId().equals(requester.getId())) {
            throw new BadRequestException("No tienes permiso para este proyecto");
        }
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private Project requireProject(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    private Task requireTask(Long id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada"));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDeletedAt() != null);
    }
}
