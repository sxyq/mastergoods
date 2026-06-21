package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.application.service.AuthService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.AuthResult> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request.phone(), request.password(), request.verifyCode()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.phone(), request.password()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.AuthResult> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
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
    public ApiResponse<VerifyCodeResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        AuthService.VerifyCodeResult result = authService.issueVerifyCode(request.phone(), request.type());
        return ApiResponse.success(new VerifyCodeResponse(result.success(), result.expireSeconds()));
    }

    @GetMapping("/users/me")
    @RequireStorePermission("dashboard:view")
    public ApiResponse<AuthService.UserProfile> me(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success(authService.me(extractBearerToken(authorization)));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("missing bearer token");
        }
        return authorization.substring("Bearer ".length());
    }

    private String extractBearerTokenOrNull(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    public record RegisterRequest(
        @NotBlank String phone,
        @NotBlank String password,
        @NotBlank String verifyCode
    ) {}

    public record LoginRequest(
        @NotBlank String phone,
        @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record VerifyCodeRequest(@NotBlank String phone, @NotBlank String type) {}

    public record VerifyCodeResponse(boolean success, int expireSeconds) {}
}
