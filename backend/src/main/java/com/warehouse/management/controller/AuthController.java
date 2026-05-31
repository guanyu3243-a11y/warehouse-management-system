package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.AuthChangePasswordRequest;
import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.dto.AuthLoginResponse;
import com.warehouse.management.dto.AuthPermissionResponse;
import com.warehouse.management.dto.AuthRegisterRequest;
import com.warehouse.management.dto.AuthUserResponse;
import com.warehouse.management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthUserResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authService.login(
                request,
                clientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success(authService.getCurrentUser(CurrentUserContext.getRequired().id()));
    }

    @GetMapping("/permissions")
    public ApiResponse<AuthPermissionResponse> permissions() {
        return ApiResponse.success(authService.getCurrentPermissions(CurrentUserContext.getRequired().id()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthChangePasswordRequest request) {
        authService.changePassword(CurrentUserContext.getRequired().id(), request);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(extractBearerToken(request));
        return ApiResponse.success();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
