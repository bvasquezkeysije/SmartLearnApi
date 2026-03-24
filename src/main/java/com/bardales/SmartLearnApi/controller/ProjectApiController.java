package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.project.ProjectRequest;
import com.bardales.SmartLearnApi.dto.project.ProjectResponse;
import com.bardales.SmartLearnApi.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectApiController {

    private final ProjectService projectService;

    public ProjectApiController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> list(@RequestParam Long requesterUserId) {
        return projectService.listProjects(requesterUserId);
    }

    @PostMapping
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId, @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable Long projectId, @RequestParam Long requesterUserId) {
        projectService.deleteProject(projectId, requesterUserId);
    }
}
