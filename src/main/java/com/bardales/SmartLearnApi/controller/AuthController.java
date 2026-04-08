package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.auth.GoogleLoginRequest;
import com.bardales.SmartLearnApi.dto.auth.GoogleLoginResponse;
import com.bardales.SmartLearnApi.dto.auth.GoogleRegisterRequest;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterRequest;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterResponse;
import com.bardales.SmartLearnApi.dto.auth.LoginRequest;
import com.bardales.SmartLearnApi.dto.auth.LoginResponse;
import com.bardales.SmartLearnApi.dto.auth.PresenceHeartbeatResponse;
import com.bardales.SmartLearnApi.dto.auth.ProfileImageUpdateRequest;
import com.bardales.SmartLearnApi.security.JwtUserPrincipal;
import com.bardales.SmartLearnApi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google/login")
    public GoogleLoginResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/google/register")
    public LoginResponse registerWithGoogle(@Valid @RequestBody GoogleRegisterRequest request) {
        return authService.registerWithGoogle(request);
    }

    @PostMapping("/register")
    public LocalRegisterResponse registerLocal(@Valid @RequestBody LocalRegisterRequest request) {
        return authService.registerLocal(request);
    }

    @GetMapping("/session")
    public LoginResponse session(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return authService.getSession(principal.userId());
    }

    @PostMapping("/profile-image")
    public LoginResponse updateProfileImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody ProfileImageUpdateRequest request) {
        return authService.updateProfileImage(principal.userId(), request);
    }

    @PostMapping("/heartbeat")
    public PresenceHeartbeatResponse heartbeat(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return authService.heartbeat(principal.userId());
    }
}
