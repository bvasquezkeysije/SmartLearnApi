package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.AndroidRelease;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.AndroidReleaseRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseActivateResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseCreateRequest;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseDeleteResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AndroidReleaseService {

    private static final Logger log = LoggerFactory.getLogger(AndroidReleaseService.class);

    private final AndroidReleaseRepository androidReleaseRepository;
    private final UserRepository userRepository;
    private final AndroidReleaseStorageService androidReleaseStorageService;

    public AndroidReleaseService(
            AndroidReleaseRepository androidReleaseRepository,
            UserRepository userRepository,
            AndroidReleaseStorageService androidReleaseStorageService) {
        this.androidReleaseRepository = androidReleaseRepository;
        this.userRepository = userRepository;
        this.androidReleaseStorageService = androidReleaseStorageService;
    }

    @Transactional(readOnly = true)
    public AndroidReleaseResponse getLatestPublicRelease() {
        AndroidRelease release = androidReleaseRepository.findByIsActiveTrue()
                .orElseThrow(() -> new NotFoundException("No hay APK activa disponible."));
        return toResponse(release);
    }

    @Transactional(readOnly = true)
    public List<AndroidReleaseResponse> listAllForAdmin(Long requesterUserId) {
        User requester = requireAdmin(requesterUserId);
        if (requester == null) {
            throw new UnauthorizedException("No autorizado");
        }
        return androidReleaseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AndroidReleaseResponse createRelease(Long requesterUserId, AndroidReleaseCreateRequest request) {
        User requester = requireAdmin(requesterUserId);

        String versionName = normalizeRequired(request.versionName(), "versionName es obligatorio");
        Integer versionCode = request.versionCode();
        if (versionCode == null || versionCode < 1) {
            throw new BadRequestException("versionCode debe ser mayor a 0");
        }
        String apkUrl = normalizeRequired(request.apkUrl(), "apkUrl es obligatorio");

        AndroidRelease release = new AndroidRelease();
        release.setVersionName(versionName);
        release.setVersionCode(versionCode);
        release.setApkUrl(apkUrl);
        release.setChecksumSha256(normalizeOptional(request.checksumSha256()));
        release.setReleaseNotes(normalizeOptional(request.releaseNotes()));
        release.setCreatedByUser(requester);
        release.setIsActive(Boolean.TRUE.equals(request.isActive()));

        if (Boolean.TRUE.equals(release.getIsActive())) {
            deactivateCurrentActiveRelease();
        }

        AndroidRelease saved = androidReleaseRepository.save(release);
        return toResponse(saved);
    }

    @Transactional
    public AndroidReleaseResponse createReleaseFromUpload(
            Long requesterUserId,
            String versionNameInput,
            Integer versionCodeInput,
            String checksumSha256Input,
            String releaseNotesInput,
            Boolean isActiveInput,
            MultipartFile apkFile) {
        User requester = requireAdmin(requesterUserId);
        String versionName = normalizeRequired(versionNameInput, "versionName es obligatorio");
        Integer versionCode = versionCodeInput;
        if (versionCode == null || versionCode < 1) {
            throw new BadRequestException("versionCode debe ser mayor a 0");
        }

        AndroidReleaseStorageService.StoredApk storedApk = androidReleaseStorageService.storeApk(apkFile);
        String storageKey = storedApk.storageKey();
        try {
            AndroidRelease release = new AndroidRelease();
            release.setVersionName(versionName);
            release.setVersionCode(versionCode);
            release.setApkUrl("pending://upload");
            release.setFileName(storedApk.fileName());
            release.setFileSizeBytes(storedApk.fileSizeBytes());
            release.setContentType(storedApk.contentType());
            release.setStorageKey(storageKey);
            release.setChecksumSha256(normalizeOptional(checksumSha256Input));
            release.setReleaseNotes(normalizeOptional(releaseNotesInput));
            release.setCreatedByUser(requester);
            release.setIsActive(Boolean.TRUE.equals(isActiveInput));

            if (Boolean.TRUE.equals(release.getIsActive())) {
                deactivateCurrentActiveRelease();
            }

            AndroidRelease saved = androidReleaseRepository.save(release);
            saved.setApkUrl(buildDownloadUrl(saved.getId()));
            AndroidRelease updated = androidReleaseRepository.save(saved);
            return toResponse(updated);
        } catch (RuntimeException ex) {
            androidReleaseStorageService.deleteIfExists(storageKey);
            throw ex;
        }
    }

    @Transactional
    public AndroidReleaseActivateResponse activateRelease(Long requesterUserId, Long releaseId) {
        requireAdmin(requesterUserId);
        AndroidRelease target = androidReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new NotFoundException("Release no encontrada."));

        if (Boolean.TRUE.equals(target.getIsActive())) {
            return new AndroidReleaseActivateResponse(
                    target.getId(),
                    target.getVersionName(),
                    target.getVersionCode(),
                    true,
                    "La release ya estaba activa.");
        }

        deactivateCurrentActiveRelease();
        target.setIsActive(true);
        AndroidRelease saved = androidReleaseRepository.save(target);

        return new AndroidReleaseActivateResponse(
                saved.getId(),
                saved.getVersionName(),
                saved.getVersionCode(),
                true,
                "Release activada correctamente.");
    }

    @Transactional
    public AndroidReleaseDeleteResponse deleteRelease(Long requesterUserId, Long releaseId) {
        requireAdmin(requesterUserId);
        AndroidRelease target = androidReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new NotFoundException("Release no encontrada."));

        String storageKey = normalizeOptional(target.getStorageKey());
        androidReleaseRepository.delete(target);

        if (storageKey != null) {
            try {
                androidReleaseStorageService.deleteIfExists(storageKey);
            } catch (RuntimeException ex) {
                log.warn("No se pudo eliminar archivo fisico APK para release {} con storageKey {}", releaseId, storageKey, ex);
            }
        }

        return new AndroidReleaseDeleteResponse(releaseId, "Release eliminada correctamente.");
    }

    @Transactional(readOnly = true)
    public AndroidReleaseFileDownload getPublicReleaseFile(Long releaseId) {
        AndroidRelease release = androidReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new NotFoundException("Release no encontrada."));
        if (!Boolean.TRUE.equals(release.getIsActive())) {
            throw new NotFoundException("Archivo APK no disponible.");
        }
        String storageKey = normalizeOptional(release.getStorageKey());
        if (storageKey == null) {
            throw new NotFoundException("Archivo APK no disponible.");
        }
        Resource resource = androidReleaseStorageService.loadAsResource(storageKey);
        return new AndroidReleaseFileDownload(
                resource,
                normalizeOptional(release.getContentType()),
                normalizeOptional(release.getFileName()));
    }

    private void deactivateCurrentActiveRelease() {
        androidReleaseRepository.findByIsActiveTrue().ifPresent(current -> {
            current.setIsActive(false);
            androidReleaseRepository.save(current);
        });
    }

    private User requireAdmin(Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new UnauthorizedException("No autorizado"));
        if (!requester.hasRole("admin")) {
            throw new UnauthorizedException("Solo los administradores pueden gestionar releases Android.");
        }

        return requester;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AndroidReleaseResponse toResponse(AndroidRelease release) {
        Long createdByUserId = release.getCreatedByUser() == null ? null : release.getCreatedByUser().getId();
        String resolvedApkUrl = normalizeOptional(release.getApkUrl());
        if (resolvedApkUrl == null && normalizeOptional(release.getStorageKey()) != null && release.getId() != null) {
            resolvedApkUrl = buildDownloadUrl(release.getId());
        }
        return new AndroidReleaseResponse(
                release.getId(),
                release.getVersionName(),
                release.getVersionCode(),
                resolvedApkUrl,
                release.getFileName(),
                release.getFileSizeBytes(),
                release.getChecksumSha256(),
                release.getReleaseNotes(),
                Boolean.TRUE.equals(release.getIsActive()),
                createdByUserId,
                release.getCreatedAt());
    }

    private String buildDownloadUrl(Long releaseId) {
        return "/api/v1/public/mobile/android/releases/" + releaseId + "/download";
    }

    public record AndroidReleaseFileDownload(Resource resource, String contentType, String fileName) {
    }
}
