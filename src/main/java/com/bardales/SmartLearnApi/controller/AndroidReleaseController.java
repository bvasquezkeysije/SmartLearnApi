package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseActivateResponse;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseCreateRequest;
import com.bardales.SmartLearnApi.dto.mobile.AndroidReleaseResponse;
import com.bardales.SmartLearnApi.security.JwtUserPrincipal;
import com.bardales.SmartLearnApi.service.AndroidReleaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PatchMapping("/api/v1/admin/mobile/android/releases/{releaseId}/activate")
    public AndroidReleaseActivateResponse activateRelease(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long releaseId) {
        return androidReleaseService.activateRelease(principal.userId(), releaseId);
    }
}
