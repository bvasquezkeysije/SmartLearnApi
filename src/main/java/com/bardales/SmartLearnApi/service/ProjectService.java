package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Project;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ProjectRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.project.ProjectRequest;
import com.bardales.SmartLearnApi.dto.project.ProjectResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(Long requesterUserId) {
        User requester = requireUser(requesterUserId);

        List<Project> projects = requester.hasRole("admin")
                ? projectRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                : projectRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(requester.getId());

        return projects.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User requester = requireUser(request.requesterUserId());

        Long ownerId = requester.getId();
        if (request.ownerUserId() != null && requester.hasRole("admin")) {
            ownerId = request.ownerUserId();
        }

        User owner = requireUser(ownerId);

        Project project = new Project();
        project.setUser(owner);
        project.setName(request.name().trim());
        project.setDescription(request.description() == null ? null : request.description().trim());

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        User requester = requireUser(request.requesterUserId());
        Project project = requireProject(projectId);
        assertProjectAccess(requester, project);

        project.setName(request.name().trim());
        project.setDescription(request.description() == null ? null : request.description().trim());

        if (request.ownerUserId() != null && requester.hasRole("admin")) {
            project.setUser(requireUser(request.ownerUserId()));
        }

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long projectId, Long requesterUserId) {
        User requester = requireUser(requesterUserId);
        Project project = requireProject(projectId);
        assertProjectAccess(requester, project);

        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
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

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getUser().getId(),
                project.getName(),
                project.getDescription(),
                project.getDeletedAt() != null);
    }
}
