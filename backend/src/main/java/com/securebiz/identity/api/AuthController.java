package com.securebiz.identity.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.securebiz.identity.config.AuthProperties;
import com.securebiz.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
@Tag(name = "Authentication", description = "Login, rotating refresh tokens and session logout")
public class AuthController {
    private static final String REFRESH_COOKIE = "sb_refresh";
    private static final String REQUEST_MARKER = "SecureBiz";

    private final AuthService authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Validates credentials, records the attempt and issues an access token plus an HttpOnly refresh cookie.")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthService.AuthResult result = authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token", description = "Consumes the current refresh token, detects replay and returns a new token pair.")
    ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @RequestHeader(name = "X-Requested-With", required = false) String requestMarker,
            HttpServletRequest request) {
        requireRequestMarker(requestMarker);
        return withRefreshCookie(authService.refresh(refreshToken, clientIp(request), request.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the current session", description = "Revokes the refresh-token family and expires the browser cookie.")
    ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @RequestHeader(name = "X-Requested-With", required = false) String requestMarker,
            HttpServletRequest request) {
        requireRequestMarker(requestMarker);
        authService.logout(refreshToken, clientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/sessions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List my sessions", description = "Lists only the authenticated user's sessions and identifies the session used by the access token.")
    List<AuthSessionResponse> sessions() {
        return authService.sessions();
    }

    @DeleteMapping("/sessions/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke one of my sessions", description = "Revokes a session and its refresh tokens only when it belongs to the authenticated user.")
    ResponseEntity<Void> revokeSession(@PathVariable UUID id) {
        authService.revokeSession(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthService.AuthResult result) {
        Duration maxAge = Duration.between(java.time.Instant.now(), result.refreshExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), maxAge).toString())
                .body(result.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }

    private static void requireRequestMarker(String marker) {
        if (!REQUEST_MARKER.equals(marker)) {
            throw new AuthException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "REQUEST_MARKER_REQUIRED", "Request marker is missing");
        }
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
