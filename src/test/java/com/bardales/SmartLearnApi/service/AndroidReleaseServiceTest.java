package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.AndroidRelease;
import com.bardales.SmartLearnApi.domain.entity.Role;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.AndroidReleaseRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseActivateResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseCreateRequest;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AndroidReleaseServiceTest {

    @Mock
    private AndroidReleaseRepository androidReleaseRepository;

    @Mock
    private UserRepository userRepository;

    private AndroidReleaseService androidReleaseService;

    @BeforeEach
    void setUp() {
        androidReleaseService = new AndroidReleaseService(androidReleaseRepository, userRepository);
    }

    @Test
    void createReleaseWithActiveFlagDeactivatesCurrentRelease() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        AndroidRelease current = new AndroidRelease();
        current.setIsActive(true);
        org.springframework.test.util.ReflectionTestUtils.setField(current, "id", 10L);
        when(androidReleaseRepository.findByIsActiveTrue()).thenReturn(Optional.of(current));

        when(androidReleaseRepository.save(any(AndroidRelease.class))).thenAnswer(invocation -> {
            AndroidRelease release = invocation.getArgument(0);
            if (release.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(release, "id", 11L);
            }
            return release;
        });

        AndroidReleaseCreateRequest request = new AndroidReleaseCreateRequest(
                "2.0.0",
                200,
                "https://cdn.smartlearn.org/apk/smartlearn-2.0.0.apk",
                null,
                "Nuevas mejoras",
                true);

        AndroidReleaseResponse response = androidReleaseService.createRelease(1L, request);

        assertEquals(11L, response.id());
        assertEquals("2.0.0", response.versionName());
        assertEquals(true, response.isActive());
        verify(androidReleaseRepository).save(current);
        verify(androidReleaseRepository, times(2)).save(any(AndroidRelease.class));
    }

    @Test
    void activateReleaseReturnsSuccess() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(androidReleaseRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        AndroidRelease target = new AndroidRelease();
        target.setVersionName("1.1.0");
        target.setVersionCode(110);
        target.setIsActive(false);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "id", 22L);

        when(androidReleaseRepository.findById(22L)).thenReturn(Optional.of(target));
        when(androidReleaseRepository.save(any(AndroidRelease.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AndroidReleaseActivateResponse response = androidReleaseService.activateRelease(1L, 22L);

        assertNotNull(response);
        assertEquals(22L, response.id());
        assertEquals(true, response.isActive());
    }

    @Test
    void listAllForAdminRejectsNonAdmin() {
        User regular = new User();
        regular.setUsername("user1");
        regular.setEmail("user1@mail.com");
        regular.setRoles(Set.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));

        assertThrows(UnauthorizedException.class, () -> androidReleaseService.listAllForAdmin(2L));
    }

    @Test
    void listAllForAdminRejectsLegacyAdminFallbackWithoutRole() {
        User legacyAdmin = new User();
        legacyAdmin.setUsername("admin");
        legacyAdmin.setEmail("admin@a21k.com");
        legacyAdmin.setRoles(Set.of());
        when(userRepository.findById(3L)).thenReturn(Optional.of(legacyAdmin));

        assertThrows(UnauthorizedException.class, () -> androidReleaseService.listAllForAdmin(3L));
    }

    private User buildAdminUser(Long id) {
        User admin = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(admin, "id", id);
        admin.setUsername("admin");
        admin.setEmail("admin@mail.com");

        Role role = new Role();
        role.setName("admin");
        admin.setRoles(Set.of(role));
        return admin;
    }
}
