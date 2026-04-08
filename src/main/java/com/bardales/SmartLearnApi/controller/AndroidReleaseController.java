package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseActivateResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseCreateRequest;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.security.JwtUserPrincipal;
import com.bardales.SmartLearnApi.service.AndroidReleaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AndroidReleaseController {

    private final AndroidReleaseService androidReleaseService;

    public AndroidReleaseController(AndroidReleaseService androidReleaseService) {
        this.androidReleaseService = androidReleaseService;
    }

    @GetMapping("/api/v1/public/mobile/android/latest")
    public AndroidReleaseResponse getLatestPublicRelease() {
        return androidReleaseService.getLatestPublicRelease();
    }

    @GetMapping("/api/v1/public/mobile/android/releases/{releaseId}/download")
    public ResponseEntity<Resource> downloadPublicRelease(@PathVariable Long releaseId) {
        AndroidReleaseService.AndroidReleaseFileDownload download = androidReleaseService.getPublicReleaseFile(releaseId);
        String fileName = download.fileName() == null || download.fileName().isBlank()
                ? "smartlearn.apk"
                : download.fileName();
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? "application/vnd.android.package-archive"
                : download.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(download.resource());
    }

    @GetMapping("/api/v1/admin/mobile/android/releases")
    public List<AndroidReleaseResponse> listAllForAdmin(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return androidReleaseService.listAllForAdmin(principal.userId());
    }

    @PostMapping("/api/v1/admin/mobile/android/releases")
    public AndroidReleaseResponse createRelease(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody AndroidReleaseCreateRequest request) {
        return androidReleaseService.createRelease(principal.userId(), request);
    }

    @PostMapping(path = "/api/v1/admin/mobile/android/releases/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AndroidReleaseResponse createReleaseByUpload(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam("apkFile") MultipartFile apkFile,
            @RequestParam("versionName") String versionName,
            @RequestParam("versionCode") Integer versionCode,
            @RequestParam(value = "checksumSha256", required = false) String checksumSha256,
            @RequestParam(value = "releaseNotes", required = false) String releaseNotes,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        return androidReleaseService.createReleaseFromUpload(
                principal.userId(),
                versionName,
                versionCode,
                checksumSha256,
                releaseNotes,
                isActive,
                apkFile);
    }

    @PatchMapping("/api/v1/admin/mobile/android/releases/{releaseId}/activate")
    public AndroidReleaseActivateResponse activateRelease(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long releaseId) {
        return androidReleaseService.activateRelease(principal.userId(), releaseId);
    }
}
