package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.RoleRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.auth.GoogleLoginRequest;
import com.bardales.SmartLearnApi.dto.auth.GoogleLoginResponse;
import com.bardales.SmartLearnApi.dto.auth.GoogleRegisterRequest;
import com.bardales.SmartLearnApi.dto.auth.LoginRequest;
import com.bardales.SmartLearnApi.dto.auth.LoginResponse;
import com.bardales.SmartLearnApi.dto.auth.PresenceHeartbeatResponse;
import com.bardales.SmartLearnApi.dto.auth.ProfileImageUpdateRequest;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import com.bardales.SmartLearnApi.security.JwtService;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AuthService {

    private static final String DEFAULT_GOOGLE_CLIENT_ID =
            "441996631829-cvhr6craa4kc3mbltlvcol2jbjsaeqi2.apps.googleusercontent.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String googleClientId;
    private final RestClient googleRestClient;
    private final RestClient googleUserInfoRestClient;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.google.client-id:}") String googleClientId) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleClientId = resolveGoogleClientId(googleClientId);
        this.googleRestClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
        this.googleUserInfoRestClient = RestClient.builder()
                .baseUrl("https://www.googleapis.com")
                .build();
    }

    private String resolveGoogleClientId(String googleClientId) {
        String normalized = googleClientId == null ? "" : googleClientId.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return DEFAULT_GOOGLE_CLIENT_ID;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = findLocalUserByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));

        ensureUserActive(user);
        if (isGoogleOnlyAccount(user)) {
            throw new UnauthorizedException(
                    "Esta cuenta usa Google. Inicia con Google o crea una password local en Perfil > Seguridad.");
        }
        if (!passwordMatches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Credenciales invalidas");
        }

        return buildLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse getSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Sesion no valida."));
        ensureUserActive(user);
        return buildLoginResponse(user);
    }

    @Transactional
    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdentity identity = verifyGoogleIdentity(request.idToken(), request.accessToken());

        Optional<User> byGoogleSubject = userRepository.findByGoogleSubject(identity.subject());
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(identity.email());
        User user = byGoogleSubject.or(() -> byEmail).orElse(null);

        if (user != null) {
            ensureUserActive(user);
            linkGoogleIdentity(user, identity);
            LoginResponse login = buildLoginResponse(user);
            return new GoogleLoginResponse(
                    false,
                    login.id(),
                    login.name(),
                    login.username(),
                    login.email(),
                    login.status(),
                    login.roles(),
                    login.token(),
                    null,
                    "Sesion iniciada con Google.",
                    login.authProvider(),
                    login.hasLocalPassword(),
                    login.profileImageData(),
                    login.profileImageScale(),
                    login.profileImageOffsetX(),
                    login.profileImageOffsetY());
        }

        String suggestedUsername = generateAvailableUsername(identity.name(), identity.email());
        return new GoogleLoginResponse(
                true,
                null,
                identity.name(),
                null,
                identity.email(),
                null,
                List.of("user"),
                null,
                suggestedUsername,
                "Cuenta de Google detectada. Completa tu registro.",
                "google",
                false,
                null,
                null,
                null,
                null);
    }

    @Transactional
    public LoginResponse registerWithGoogle(GoogleRegisterRequest request) {
        GoogleIdentity identity = verifyGoogleIdentity(request.idToken(), request.accessToken());

        Optional<User> byGoogleSubject = userRepository.findByGoogleSubject(identity.subject());
        if (byGoogleSubject.isPresent()) {
            User user = byGoogleSubject.get();
            ensureUserActive(user);
            return buildLoginResponse(user);
        }

        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(identity.email());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            ensureUserActive(user);
            linkGoogleIdentity(user, identity);
            return buildLoginResponse(user);
        }

        String normalizedUsername = normalizeUsername(request.username());
        if (normalizedUsername.length() < 3) {
            throw new BadRequestException("El username debe tener al menos 3 caracteres validos.");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BadRequestException("El username ya esta en uso.");
        }

        String finalName = request.name() == null ? "" : request.name().trim();
        if (finalName.isBlank()) {
            finalName = identity.name();
        }
        if (finalName == null || finalName.isBlank()) {
            finalName = normalizedUsername;
        }

        User user = new User();
        user.setName(finalName);
        user.setUsername(normalizedUsername);
        user.setEmail(identity.email());
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(1);
        user.setAuthProvider("google");
        user.setGoogleSubject(identity.subject());
        user.setGooglePictureUrl(identity.pictureUrl());
        user.setHasLocalPassword(false);
        roleRepository.findByNameIgnoreCase("user").ifPresent(role -> user.getRoles().add(role));
        User saved = userRepository.save(user);

        return buildLoginResponse(saved);
    }

    private Optional<User> findLocalUserByIdentifier(String identifier) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier);
    }

    private void ensureUserActive(User user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new UnauthorizedException("Usuario inactivo");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        List<String> roles = resolveRoles(user);
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                roles,
                jwtService.generateToken(user, roles),
                normalizeAuthProvider(user.getAuthProvider()),
                hasLocalPassword(user),
                user.getProfileImageData(),
                user.getProfileImageScale(),
                user.getProfileImageOffsetX(),
                user.getProfileImageOffsetY());
    }

    @Transactional
    public LoginResponse updateProfileImage(Long userId, ProfileImageUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado."));
        ensureUserActive(user);

        String profileImageData = trimOrNull(request.profileImageData());
        if (profileImageData != null && profileImageData.length() > 7_000_000) {
            throw new BadRequestException("La foto de perfil es demasiado grande.");
        }

        Double scale = clampScale(request.profileImageScale());
        Double offsetX = clampOffsetX(request.profileImageOffsetX(), scale);
        Double offsetY = 0d;

        user.setProfileImageData(profileImageData);
        user.setProfileImageScale(profileImageData == null ? null : scale);
        user.setProfileImageOffsetX(profileImageData == null ? null : offsetX);
        user.setProfileImageOffsetY(profileImageData == null ? null : offsetY);
        User saved = userRepository.save(user);

        return buildLoginResponse(saved);
    }

    @Transactional
    public PresenceHeartbeatResponse heartbeat(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado."));
        ensureUserActive(user);

        LocalDateTime now = LocalDateTime.now();
        user.setLastSeenAt(now);
        userRepository.save(user);

        return new PresenceHeartbeatResponse(now, "Presencia actualizada");
    }

    private boolean isGoogleOnlyAccount(User user) {
        return "google".equalsIgnoreCase(normalizeAuthProvider(user.getAuthProvider()))
                && !hasLocalPassword(user);
    }

    private boolean hasLocalPassword(User user) {
        if (user.getHasLocalPassword() != null) {
            return Boolean.TRUE.equals(user.getHasLocalPassword());
        }
        return !"google".equalsIgnoreCase(normalizeAuthProvider(user.getAuthProvider()));
    }

    private String normalizeAuthProvider(String authProvider) {
        return authProvider == null ? "" : authProvider.trim();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double clampScale(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return 1d;
        }
        return Math.max(1d, Math.min(3d, value));
    }

    private Double clampOffsetX(Double value, Double scale) {
        double safeScale = scale == null || !Double.isFinite(scale) ? 1d : scale;
        double limit = Math.max(0d, ((safeScale - 1d) / 2d) * 100d);
        double safeValue = value == null || !Double.isFinite(value) ? 0d : value;
        return Math.max(-limit, Math.min(limit, safeValue));
    }

    private List<String> resolveRoles(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (roles.isEmpty()) {
            boolean isAdminUser = "admin".equalsIgnoreCase(user.getUsername())
                    || "admin@a21k.com".equalsIgnoreCase(user.getEmail());
            roles = isAdminUser ? List.of("admin") : List.of("user");
        }
        return roles;
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

    private GoogleIdentity verifyGoogleIdentity(String idToken, String accessToken) {
        String rawIdToken = idToken == null ? "" : idToken.trim();
        String rawAccessToken = accessToken == null ? "" : accessToken.trim();
        if (rawIdToken.isBlank() && rawAccessToken.isBlank()) {
            throw new BadRequestException("idToken o accessToken de Google es obligatorio.");
        }
        if (googleClientId.isBlank()) {
            throw new BadRequestException("Google Sign-In no esta configurado en el servidor.");
        }

        if (!rawIdToken.isBlank()) {
            return verifyGoogleIdToken(rawIdToken);
        }
        return verifyGoogleAccessToken(rawAccessToken);
    }

    private GoogleIdentity verifyGoogleIdToken(String rawToken) {

        Map<String, Object> payload;
        try {
            payload = googleRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo")
                            .queryParam("id_token", rawToken)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException ex) {
            throw new UnauthorizedException("No se pudo validar el token de Google.");
        }

        if (payload == null || payload.isEmpty()) {
            throw new UnauthorizedException("Token de Google invalido.");
        }

        String audience = asString(payload.get("aud"));
        if (!googleClientId.equals(audience)) {
            throw new UnauthorizedException("El token de Google no corresponde a esta aplicacion.");
        }

        long exp = parseEpochSeconds(payload.get("exp"));
        if (exp > 0 && Instant.now().getEpochSecond() >= exp) {
            throw new UnauthorizedException("El token de Google expiro.");
        }

        String subject = asString(payload.get("sub"));
        String email = asString(payload.get("email"));
        boolean emailVerified = parseBoolean(payload.get("email_verified"));
        if (subject.isBlank() || email.isBlank()) {
            throw new UnauthorizedException("Token de Google invalido.");
        }
        if (!emailVerified) {
            throw new UnauthorizedException("Tu correo de Google no esta verificado.");
        }

        String name = asString(payload.get("name"));
        if (name.isBlank()) {
            int at = email.indexOf('@');
            name = at > 0 ? email.substring(0, at) : email;
        }
        String picture = asString(payload.get("picture"));

        return new GoogleIdentity(subject, email, name, picture);
    }

    private GoogleIdentity verifyGoogleAccessToken(String accessToken) {
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = googleRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException ex) {
            throw new UnauthorizedException("No se pudo validar el access token de Google.");
        }

        if (tokenInfo == null || tokenInfo.isEmpty()) {
            throw new UnauthorizedException("Access token de Google invalido.");
        }

        String aud = asString(tokenInfo.get("aud"));
        String issuedTo = asString(tokenInfo.get("issued_to"));
        String azp = asString(tokenInfo.get("azp"));
        boolean audienceMatches = googleClientId.equals(aud)
                || googleClientId.equals(issuedTo)
                || googleClientId.equals(azp);
        if (!audienceMatches) {
            throw new UnauthorizedException("El access token de Google no corresponde a esta aplicacion.");
        }

        String expiresInRaw = asString(tokenInfo.get("expires_in"));
        if (!expiresInRaw.isBlank()) {
            try {
                if (Long.parseLong(expiresInRaw) <= 0) {
                    throw new UnauthorizedException("El access token de Google expiro.");
                }
            } catch (NumberFormatException ignored) {
                throw new UnauthorizedException("No se pudo validar la vigencia del access token de Google.");
            }
        }

        Map<String, Object> userInfo;
        try {
            userInfo = googleUserInfoRestClient
                    .get()
                    .uri("/oauth2/v3/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException ex) {
            throw new UnauthorizedException("No se pudo obtener el perfil de Google.");
        }

        if (userInfo == null || userInfo.isEmpty()) {
            throw new UnauthorizedException("No se pudo obtener el perfil de Google.");
        }

        String subject = asString(userInfo.get("sub"));
        String email = asString(userInfo.get("email"));
        boolean emailVerified = parseBoolean(userInfo.get("email_verified"));
        if (subject.isBlank() || email.isBlank()) {
            throw new UnauthorizedException("Access token de Google invalido.");
        }
        if (!emailVerified) {
            throw new UnauthorizedException("Tu correo de Google no esta verificado.");
        }

        String name = asString(userInfo.get("name"));
        if (name.isBlank()) {
            int at = email.indexOf('@');
            name = at > 0 ? email.substring(0, at) : email;
        }
        String picture = asString(userInfo.get("picture"));

        return new GoogleIdentity(subject, email, name, picture);
    }

    private void linkGoogleIdentity(User user, GoogleIdentity identity) {
        boolean changed = false;
        if (user.getGoogleSubject() == null || user.getGoogleSubject().isBlank()) {
            user.setGoogleSubject(identity.subject());
            changed = true;
        }
        if (user.getGooglePictureUrl() == null || user.getGooglePictureUrl().isBlank()) {
            user.setGooglePictureUrl(identity.pictureUrl());
            changed = true;
        }
        if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
            user.setAuthProvider("google");
            changed = true;
        }
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private String generateAvailableUsername(String name, String email) {
        String base = normalizeUsername(name);
        if (base.length() < 3) {
            String emailLocalPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            base = normalizeUsername(emailLocalPart);
        }
        if (base.length() < 3) {
            base = "user";
        }

        String prefix = base.length() > 24 ? base.substring(0, 24) : base;
        if (!userRepository.existsByUsernameIgnoreCase(prefix)) {
            return prefix;
        }

        for (int i = 1; i < 10000; i++) {
            String suffix = String.valueOf(i);
            int maxPrefixLength = Math.max(1, 30 - suffix.length());
            String candidateBase = prefix.length() > maxPrefixLength ? prefix.substring(0, maxPrefixLength) : prefix;
            String candidate = candidateBase + suffix;
            if (!userRepository.existsByUsernameIgnoreCase(candidate)) {
                return candidate;
            }
        }

        String fallback = "user" + (System.currentTimeMillis() % 100000);
        return fallback.toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "");
        if (normalized.length() > 30) {
            return normalized.substring(0, 30);
        }
        return normalized;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private long parseEpochSeconds(Object value) {
        String raw = asString(value);
        if (raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        String raw = asString(value);
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private record GoogleIdentity(
            String subject,
            String email,
            String name,
            String pictureUrl) {
    }
}
