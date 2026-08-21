package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.auth.V2AuthDtos;
import com.zhihuiji.backend.application.service.AuthService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/v2/auth")
public class V2AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public V2AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<V2AuthDtos.AuthResponse> register(@Valid @RequestBody V2AuthDtos.RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request.phone(), request.password(), request.verifyCode());
        return ApiResponse.success(toAuthResponse(result));
    }

    @PostMapping("/login")
    public ApiResponse<V2AuthDtos.AuthResponse> login(@Valid @RequestBody V2AuthDtos.LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.phone(), request.password());
        return ApiResponse.success(toAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ApiResponse<V2AuthDtos.AuthResponse> refresh(@Valid @RequestBody V2AuthDtos.RefreshRequest request) {
        AuthService.AuthResult result = authService.refresh(request.refreshToken());
        return ApiResponse.success(toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerTokenOrNull(authorization);
        if (token != null) {
            authService.logout(token);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/verify-code")
    public ApiResponse<V2AuthDtos.VerifyCodeResponse> verifyCode(@Valid @RequestBody V2AuthDtos.VerifyCodeRequest request) {
        AuthService.VerifyCodeResult result = authService.issueVerifyCode(request.phone(), request.type());
        return ApiResponse.success(new V2AuthDtos.VerifyCodeResponse(result.success(), result.expireSeconds()));
    }

    @GetMapping("/users/me")
    @RequireStorePermission("dashboard:view")
    public ApiResponse<V2AuthDtos.UserProfileResponse> me(@RequestHeader("Authorization") String authorization) {
        AuthService.UserProfile profile = authService.me(extractBearerToken(authorization));
        return ApiResponse.success(new V2AuthDtos.UserProfileResponse(
            profile.id(), profile.phone(), profile.nickname(), profile.status()
        ));
    }

    private V2AuthDtos.AuthResponse toAuthResponse(AuthService.AuthResult result) {
        return new V2AuthDtos.AuthResponse(result.userId(), result.token(), result.refreshToken(), result.expiresIn());
    }

    private String extractBearerToken(String authorization) {
        String token = extractBearerTokenOrNull(authorization);
        if (token == null) {
            throw new IllegalArgumentException("missing bearer token");
        }
        return token;
    }

    private String extractBearerTokenOrNull(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
