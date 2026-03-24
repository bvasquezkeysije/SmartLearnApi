package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Person;
import com.bardales.SmartLearnApi.domain.entity.Role;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.PersonRepository;
import com.bardales.SmartLearnApi.domain.repository.RoleRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.user.UserCreateRequest;
import com.bardales.SmartLearnApi.dto.user.UserPasswordUpdateRequest;
import com.bardales.SmartLearnApi.dto.user.UserPasswordUpdateResponse;
import com.bardales.SmartLearnApi.dto.user.UserResponse;
import com.bardales.SmartLearnApi.dto.user.UserUpdateRequest;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PersonRepository personRepository,
            RoleRepository roleRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Long requesterUserId) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse createUser(Long requesterUserId, UserCreateRequest request) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("El email ya existe");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("El username ya existe");
        }

        Role role = roleRepository
                .findByNameIgnoreCase(request.role().trim())
                .orElseThrow(() -> new BadRequestException("Rol invalido"));

        Person person = new Person();
        person.setFirstName(request.firstName().trim());
        person.setLastName(request.lastName().trim());
        person = personRepository.save(person);

        User user = new User();
        user.setPerson(person);
        user.setName((request.firstName().trim() + " " + request.lastName().trim()).trim());
        user.setUsername(request.username().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setHasLocalPassword(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setStatus(1);
        user.setRoles(new HashSet<>(List.of(role)));

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long requesterUserId, Long userId, UserUpdateRequest request) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        User user = requireUser(userId);
        Long currentUserId = user.getId();

        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new BadRequestException("El email ya existe");
            }
        });

        userRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new BadRequestException("El username ya existe");
            }
        });

        Role role = roleRepository
                .findByNameIgnoreCase(request.role().trim())
                .orElseThrow(() -> new BadRequestException("Rol invalido"));

        Person person = user.getPerson();
        if (person == null) {
            person = new Person();
        }
        person.setFirstName(request.firstName().trim());
        person.setLastName(request.lastName().trim());
        person = personRepository.save(person);

        user.setPerson(person);
        user.setName((request.firstName().trim() + " " + request.lastName().trim()).trim());
        user.setUsername(request.username().trim());
        user.setEmail(email);
        user.setRoles(new HashSet<>(List.of(role)));

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setHasLocalPassword(true);
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse activateUser(Long requesterUserId, Long userId) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        User user = requireUser(userId);
        user.setStatus(1);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(Long requesterUserId, Long userId) {
        User requester = requireUser(requesterUserId);
        assertAdmin(requester);

        if (requester.getId().equals(userId)) {
            throw new BadRequestException("No puedes inactivar tu propio usuario");
        }

        User user = requireUser(userId);
        user.setStatus(0);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserPasswordUpdateResponse updateUserPassword(
            Long requesterUserId,
            Long userId,
            UserPasswordUpdateRequest request) {
        User requester = requireUser(requesterUserId);
        boolean ownAccount = requester.getId().equals(userId);
        if (!ownAccount) {
            assertAdmin(requester);
        }

        User user = requireUser(userId);
        boolean requiresCurrentPassword = !isGoogleOnlyWithoutLocalPassword(user);
        if (requiresCurrentPassword) {
            String currentPassword = request.currentPassword() == null ? "" : request.currentPassword().trim();
            if (currentPassword.isBlank()) {
                throw new BadRequestException("Ingresa tu password actual.");
            }
            if (!passwordMatches(currentPassword, user.getPassword())) {
                throw new BadRequestException("La password actual no es correcta.");
            }
        }

        String newPassword = request.newPassword() == null ? "" : request.newPassword().trim();
        if (newPassword.length() < 3) {
            throw new BadRequestException("La nueva password debe tener minimo 3 caracteres.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setHasLocalPassword(true);
        userRepository.save(user);

        return new UserPasswordUpdateResponse("Password actualizada.", true);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void assertAdmin(User user) {
        boolean hasAdminRole = user.hasRole("admin");
        boolean isAdminIdentity = user.getUsername() != null
                && user.getUsername().equalsIgnoreCase("admin");
        if (!isAdminIdentity && user.getEmail() != null) {
            isAdminIdentity = user.getEmail().equalsIgnoreCase("admin@a21k.com");
        }

        if (!hasAdminRole && !isAdminIdentity) {
            throw new BadRequestException("Permiso denegado");
        }
    }

    private boolean isGoogleOnlyWithoutLocalPassword(User user) {
        String provider = user.getAuthProvider() == null ? "" : user.getAuthProvider().trim();
        return "google".equalsIgnoreCase(provider) && !Boolean.TRUE.equals(user.getHasLocalPassword());
    }

    private boolean passwordMatches(String raw, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }

        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, stored);
        }

        return raw.equals(stored);
    }

    private UserResponse toResponse(User user) {
        String firstName = user.getPerson() != null ? user.getPerson().getFirstName() : "";
        String lastName = user.getPerson() != null ? user.getPerson().getLastName() : "";
        List<String> roles = user.getRoles().stream().map(r -> r.getName()).sorted().toList();
        LocalDateTime lastSeenAt = user.getLastSeenAt();
        boolean online = lastSeenAt != null && lastSeenAt.isAfter(LocalDateTime.now().minusMinutes(2));

        return new UserResponse(
                user.getId(),
                firstName,
                lastName,
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
            online,
            lastSeenAt,
                roles);
    }
}
