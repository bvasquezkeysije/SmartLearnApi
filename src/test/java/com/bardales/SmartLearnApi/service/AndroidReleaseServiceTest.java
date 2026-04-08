package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseDeleteResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AndroidReleaseServiceTest {

    @Mock
    private AndroidReleaseRepository androidReleaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AndroidReleaseStorageService androidReleaseStorageService;

    private AndroidReleaseService androidReleaseService;

    @BeforeEach
    void setUp() {
        androidReleaseService = new AndroidReleaseService(androidReleaseRepository, userRepository, androidReleaseStorageService);
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

    @Test
    void createReleaseFromUploadBuildsDownloadUrl() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(androidReleaseRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        MockMultipartFile apkFile = new MockMultipartFile(
                "apkFile",
                "smartlearn.apk",
                "application/vnd.android.package-archive",
                new byte[] {1, 2, 3});
        AndroidReleaseStorageService.StoredApk storedApk = new AndroidReleaseStorageService.StoredApk(
                "stored-key.apk",
                "smartlearn.apk",
                3L,
                "application/vnd.android.package-archive");
        when(androidReleaseStorageService.storeApk(apkFile)).thenReturn(storedApk);

        when(androidReleaseRepository.save(any(AndroidRelease.class))).thenAnswer(invocation -> {
            AndroidRelease release = invocation.getArgument(0);
            if (release.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(release, "id", 77L);
            }
            return release;
        });

        AndroidReleaseResponse response = androidReleaseService.createReleaseFromUpload(
                1L,
                "1.0.0",
                100,
                null,
                "Upload release",
                true,
                apkFile);

        assertEquals(77L, response.id());
        assertEquals("/api/v1/public/mobile/android/releases/77/download", response.apkUrl());
        assertEquals("smartlearn.apk", response.fileName());
        assertEquals(3L, response.fileSizeBytes());
    }

    @Test
    void deleteInactiveReleaseRemovesRecord() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AndroidRelease inactive = new AndroidRelease();
        inactive.setIsActive(false);
        org.springframework.test.util.ReflectionTestUtils.setField(inactive, "id", 31L);
        when(androidReleaseRepository.findById(31L)).thenReturn(Optional.of(inactive));

        AndroidReleaseDeleteResponse response = androidReleaseService.deleteRelease(1L, 31L);

        assertEquals(31L, response.releaseId());
        verify(androidReleaseRepository).delete(inactive);
        verify(androidReleaseStorageService, never()).deleteIfExists(any());
    }

    @Test
    void deleteActiveReleaseRemovesRecord() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AndroidRelease active = new AndroidRelease();
        active.setIsActive(true);
        org.springframework.test.util.ReflectionTestUtils.setField(active, "id", 32L);
        when(androidReleaseRepository.findById(32L)).thenReturn(Optional.of(active));

        AndroidReleaseDeleteResponse response = androidReleaseService.deleteRelease(1L, 32L);

        assertEquals(32L, response.releaseId());
        verify(androidReleaseRepository).delete(active);
    }

    @Test
    void deleteReleaseWithPhysicalFileDeletesStorage() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AndroidRelease release = new AndroidRelease();
        release.setStorageKey("apk/releases/file-1.apk");
        org.springframework.test.util.ReflectionTestUtils.setField(release, "id", 33L);
        when(androidReleaseRepository.findById(33L)).thenReturn(Optional.of(release));

        AndroidReleaseDeleteResponse response = androidReleaseService.deleteRelease(1L, 33L);

        assertEquals(33L, response.releaseId());
        verify(androidReleaseRepository).delete(release);
        verify(androidReleaseStorageService).deleteIfExists("apk/releases/file-1.apk");
    }

    @Test
    void deleteReleaseNotFoundReturns404() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(androidReleaseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> androidReleaseService.deleteRelease(1L, 404L));
    }

    @Test
    void deleteReleaseRejectsNonAdmin() {
        User regular = new User();
        regular.setUsername("user1");
        regular.setEmail("user1@mail.com");
        regular.setRoles(Set.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));

        assertThrows(UnauthorizedException.class, () -> androidReleaseService.deleteRelease(2L, 1L));
    }

    @Test
    void deleteReleaseKeepsDbConsistencyWhenPhysicalDeleteFails() {
        User admin = buildAdminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AndroidRelease release = new AndroidRelease();
        release.setStorageKey("apk/releases/file-2.apk");
        org.springframework.test.util.ReflectionTestUtils.setField(release, "id", 34L);
        when(androidReleaseRepository.findById(34L)).thenReturn(Optional.of(release));
        doThrow(new RuntimeException("storage down")).when(androidReleaseStorageService).deleteIfExists("apk/releases/file-2.apk");

        AndroidReleaseDeleteResponse response = androidReleaseService.deleteRelease(1L, 34L);

        assertEquals(34L, response.releaseId());
        verify(androidReleaseRepository).delete(release);
        verify(androidReleaseStorageService).deleteIfExists("apk/releases/file-2.apk");
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
