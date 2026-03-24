package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Permission;
import com.bardales.SmartLearnApi.domain.entity.Role;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.PermissionRepository;
import com.bardales.SmartLearnApi.domain.repository.RoleRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.role.RoleCreateRequest;
import com.bardales.SmartLearnApi.dto.role.RoleItemResponse;
import com.bardales.SmartLearnApi.dto.role.RoleManagementResponse;
import com.bardales.SmartLearnApi.dto.role.RolePermissionsUpdateRequest;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RoleManagementResponse getManagement(Long requesterUserId) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        List<Permission> allPermissions = permissionRepository.findAllByOrderByNameAsc();
        List<String> availablePermissions =
                allPermissions.stream().map(Permission::getName).filter(name -> name != null && !name.isBlank()).toList();

        List<Role> rolesEntities = roleRepository.findAllByOrderByNameAsc();
        ensureDefaultRolePermissions(rolesEntities, allPermissions);

        List<RoleItemResponse> roles = rolesEntities.stream()
                .map(this::toRoleItem)
                .toList();

        return new RoleManagementResponse(roles, availablePermissions);
    }

    @Transactional
    public RoleItemResponse createRole(Long requesterUserId, RoleCreateRequest request) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        String roleName = request.roleName().trim().toLowerCase(Locale.ROOT);
        if (roleName.isBlank()) {
            throw new BadRequestException("roleName es obligatorio");
        }

        if (roleRepository.existsByNameIgnoreCase(roleName)) {
            throw new BadRequestException("El rol ya existe");
        }

        Role role = new Role();
        role.setName(roleName);
        role.setGuardName("web");
        role.setPermissions(new LinkedHashSet<>());
        role = roleRepository.save(role);

        return toRoleItem(role);
    }

    @Transactional
    public RoleItemResponse updateRolePermissions(Long requesterUserId, Long roleId, RolePermissionsUpdateRequest request) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Rol no encontrado"));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByNameAsc();
        Map<String, Permission> permissionsByName = allPermissions.stream()
                .filter(permission -> permission.getName() != null)
                .collect(Collectors.toMap(
                        permission -> permission.getName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, second) -> first));

        Set<String> requestedNames = request.permissions().stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedNames.isEmpty()) {
            throw new BadRequestException("Debes seleccionar al menos un permiso");
        }

        Set<Permission> selected = new LinkedHashSet<>();
        for (String permissionName : requestedNames) {
            Permission permission = permissionsByName.get(permissionName);
            if (permission == null) {
                throw new BadRequestException("Permiso invalido: " + permissionName);
            }
            selected.add(permission);
        }

        role.setPermissions(selected);
        role = roleRepository.save(role);
        return toRoleItem(role);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void assertAdmin(User user) {
        boolean hasAdminRole = user.hasRole("admin");
        boolean isAdminIdentity = user.getUsername() != null && user.getUsername().equalsIgnoreCase("admin");
        if (!isAdminIdentity && user.getEmail() != null) {
            isAdminIdentity = user.getEmail().equalsIgnoreCase("admin@a21k.com");
        }

        if (!hasAdminRole && !isAdminIdentity) {
            throw new BadRequestException("Permiso denegado");
        }
    }

    private RoleItemResponse toRoleItem(Role role) {
        List<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new RoleItemResponse(role.getId(), role.getName(), permissions);
    }

    private void ensureDefaultRolePermissions(List<Role> roles, List<Permission> allPermissions) {
        if (roles.isEmpty() || allPermissions.isEmpty()) {
            return;
        }

        boolean changed = false;
        Set<Permission> allPermissionSet = new LinkedHashSet<>(allPermissions);
        Set<Permission> defaultUserPermissions = allPermissions.stream()
                .filter(permission -> {
                    String name = permission.getName();
                    return name != null && name.startsWith("portal.");
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Role role : roles) {
            String roleName = role.getName() == null ? "" : role.getName().trim().toLowerCase(Locale.ROOT);
            Set<Permission> current = role.getPermissions();
            if (current == null || current.isEmpty()) {
                if ("admin".equals(roleName)) {
                    role.setPermissions(new LinkedHashSet<>(allPermissionSet));
                    changed = true;
                } else if ("user".equals(roleName)) {
                    role.setPermissions(new LinkedHashSet<>(defaultUserPermissions));
                    changed = true;
                }
            }
        }

        if (changed) {
            roleRepository.saveAll(roles);
        }
    }
}
