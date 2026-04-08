package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Role;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.RoleRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterRequest;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, "");
    }

    @Test
    void registerLocalCreatesUserAndReturnsToken() {
        LocalRegisterRequest request = new LocalRegisterRequest(
                "Nuevo Usuario",
                "new_user",
                "new_user@mail.com",
                "Secret123",
                "Secret123");

        when(userRepository.existsByEmailIgnoreCase("new_user@mail.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);

        Role role = new Role();
        role.setName("user");
        when(roleRepository.findByNameIgnoreCase("user")).thenReturn(Optional.of(role));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 99L);
            return user;
        });
        when(jwtService.generateToken(any(User.class), any())).thenReturn("jwt-token");

        LocalRegisterResponse response = authService.registerLocal(request);

        assertEquals(99L, response.id());
        assertEquals("new_user", response.username());
        assertEquals("new_user@mail.com", response.email());
        assertEquals("jwt-token", response.token());
        assertEquals("Usuario registrado correctamente.", response.message());
        assertTrue(Boolean.TRUE.equals(response.hasLocalPassword()));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerLocalRejectsDuplicateEmail() {
        LocalRegisterRequest request = new LocalRegisterRequest(
                "Duplicado",
                "duplicate_user",
                "dup@mail.com",
                "Secret123",
                "Secret123");

        when(userRepository.existsByEmailIgnoreCase("dup@mail.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.registerLocal(request));
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("correo"));
    }

    @Test
    void registerLocalRejectsPasswordMismatch() {
        LocalRegisterRequest request = new LocalRegisterRequest(
                "Mismatch",
                "mismatch_user",
                "mismatch@mail.com",
                "Secret123",
                "Other123");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.registerLocal(request));
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("coinciden"));
    }

    @Test
    void registerLocalRejectsMissingConfirmPassword() {
        LocalRegisterRequest request = new LocalRegisterRequest(
                "Sin Confirmacion",
                "without_confirm",
                "without_confirm@mail.com",
                "Secret123",
                null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.registerLocal(request));
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Confirmar"));
    }
}
