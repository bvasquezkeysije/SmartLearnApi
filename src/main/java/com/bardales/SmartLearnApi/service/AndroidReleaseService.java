package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.AndroidRelease;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.AndroidReleaseRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseActivateResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseCreateRequest;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AndroidReleaseService {

    private final AndroidReleaseRepository androidReleaseRepository;
    private final UserRepository userRepository;

    public AndroidReleaseService(AndroidReleaseRepository androidReleaseRepository, UserRepository userRepository) {
        this.androidReleaseRepository = androidReleaseRepository;
        this.userRepository = userRepository;
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
        return new AndroidReleaseResponse(
                release.getId(),
                release.getVersionName(),
                release.getVersionCode(),
                release.getApkUrl(),
                release.getChecksumSha256(),
                release.getReleaseNotes(),
                Boolean.TRUE.equals(release.getIsActive()),
                createdByUserId,
                release.getCreatedAt());
    }
}
