package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.role.RoleCreateRequest;
import com.bardales.SmartLearnApi.dto.role.RoleItemResponse;
import com.bardales.SmartLearnApi.dto.role.RoleManagementResponse;
import com.bardales.SmartLearnApi.dto.role.RolePermissionsUpdateRequest;
import com.bardales.SmartLearnApi.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleApiController {

    private final RoleService roleService;

    public RoleApiController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/management")
    public RoleManagementResponse management(@RequestParam Long requesterUserId) {
        return roleService.getManagement(requesterUserId);
    }

    @PostMapping
    public RoleItemResponse create(@RequestParam Long requesterUserId, @Valid @RequestBody RoleCreateRequest request) {
        return roleService.createRole(requesterUserId, request);
    }

    @PatchMapping("/{roleId}/permissions")
    public RoleItemResponse updatePermissions(
            @PathVariable Long roleId,
            @RequestParam Long requesterUserId,
            @Valid @RequestBody RolePermissionsUpdateRequest request) {
        return roleService.updateRolePermissions(requesterUserId, roleId, request);
    }
}
