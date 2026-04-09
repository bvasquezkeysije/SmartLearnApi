package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Role;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.RoleRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.auth.GoogleLoginRequest;
import com.bardales.SmartLearnApi.dto.auth.GoogleLoginResponse;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterRequest;
import com.bardales.SmartLearnApi.dto.auth.LocalRegisterResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import com.bardales.SmartLearnApi.security.JwtService;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RestClient googleRestClient;

    @Mock
    private RestClient googleUserInfoRestClient;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            jwtService,
            "test-client-id",
            googleRestClient,
            googleUserInfoRestClient);
    }

    @Test
    void loginWithGoogleAccessTokenFallsBackToTokenInfoWhenUserInfoFails() {
        Map<String, Object> tokenInfo = Map.of(
            "aud", "test-client-id",
            "expires_in", "3600",
            "user_id", "google-sub-123",
            "email", "fallback.user@gmail.com",
            "verified_email", true);

        mockTokenInfoResponse(tokenInfo);
        mockUserInfoFailure();

        User existing = new User();
        ReflectionTestUtils.setField(existing, "id", 10L);
        existing.setName("Fallback User");
        existing.setUsername("fallbackuser");
        existing.setEmail("fallback.user@gmail.com");
        existing.setStatus(1);
        existing.setRoles(new HashSet<>());
        existing.setAuthProvider("local");

        when(userRepository.findByGoogleSubject("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("fallback.user@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);
        when(jwtService.generateToken(any(User.class), any())).thenReturn("jwt-fallback");

        GoogleLoginResponse response = authService.loginWithGoogle(new GoogleLoginRequest(null, "valid-access-token"));

        assertTrue(!response.requiresRegistration());
        assertEquals("fallback.user@gmail.com", response.email());
        assertEquals("jwt-fallback", response.token());
    }

    @Test
    void loginWithGoogleAccessTokenRejectsWhenFallbackLacksIdentityFields() {
        Map<String, Object> tokenInfo = Map.of(
            "aud", "test-client-id",
            "expires_in", "3600",
            "verified_email", true);

        mockTokenInfoResponse(tokenInfo);
        mockUserInfoFailure();

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.loginWithGoogle(new GoogleLoginRequest(null, "valid-access-token")));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("invalido"));
    }

    @Test
    void loginWithGoogleAccessTokenFallsBackWhenUserInfoIsEmpty() {
        Map<String, Object> tokenInfo = Map.of(
            "aud", "test-client-id",
            "expires_in", "3600",
            "sub", "google-sub-777",
            "email", "empty.user@gmail.com",
            "email_verified", true);

        mockTokenInfoResponse(tokenInfo);
        mockUserInfoEmpty();

        User existing = new User();
        ReflectionTestUtils.setField(existing, "id", 11L);
        existing.setName("Empty User");
        existing.setUsername("emptyuser");
        existing.setEmail("empty.user@gmail.com");
        existing.setStatus(1);
        existing.setRoles(new HashSet<>());
        existing.setAuthProvider("local");

        when(userRepository.findByGoogleSubject("google-sub-777")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("empty.user@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);
        when(jwtService.generateToken(any(User.class), any())).thenReturn("jwt-empty");

        GoogleLoginResponse response = authService.loginWithGoogle(new GoogleLoginRequest(null, "valid-access-token"));

        assertTrue(!response.requiresRegistration());
        assertEquals("empty.user@gmail.com", response.email());
        assertEquals("jwt-empty", response.token());
    }

    @Test
    void loginWithGoogleAccessTokenRejectsWhenEmailIsNotVerifiedInFallback() {
        Map<String, Object> tokenInfo = Map.of(
            "aud", "test-client-id",
            "expires_in", "3600",
            "user_id", "google-sub-999",
            "email", "not.verified@gmail.com",
            "verified_email", false);

        mockTokenInfoResponse(tokenInfo);
        mockUserInfoFailure();

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.loginWithGoogle(new GoogleLoginRequest(null, "valid-access-token")));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("no esta verificado"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockTokenInfoResponse(Map<String, Object> tokenInfo) {
        RestClient.RequestHeadersUriSpec tokenGet = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec tokenResponse = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

        doReturn(tokenGet).when(googleRestClient).get();
        when(tokenGet.uri(any(Function.class))).thenReturn(tokenGet);
        when(tokenGet.retrieve()).thenReturn(tokenResponse);
        when(tokenResponse.body(any(ParameterizedTypeReference.class))).thenReturn(tokenInfo);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockUserInfoFailure() {
        RestClient.RequestHeadersUriSpec userGet = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec userHeaders = org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec userResponse = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

        doReturn(userGet).when(googleUserInfoRestClient).get();
        when(userGet.uri("/oauth2/v3/userinfo")).thenReturn(userHeaders);
        when(userHeaders.header(eq(HttpHeaders.AUTHORIZATION), any(String[].class))).thenReturn(userHeaders);
        when(userHeaders.retrieve()).thenReturn(userResponse);
        when(userResponse.body(any(ParameterizedTypeReference.class))).thenThrow(new RestClientException("userinfo down"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockUserInfoEmpty() {
        RestClient.RequestHeadersUriSpec userGet = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec userHeaders = org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec userResponse = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

        doReturn(userGet).when(googleUserInfoRestClient).get();
        when(userGet.uri("/oauth2/v3/userinfo")).thenReturn(userHeaders);
        when(userHeaders.header(eq(HttpHeaders.AUTHORIZATION), any(String[].class))).thenReturn(userHeaders);
        when(userHeaders.retrieve()).thenReturn(userResponse);
        when(userResponse.body(any(ParameterizedTypeReference.class))).thenReturn(Map.of());
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
