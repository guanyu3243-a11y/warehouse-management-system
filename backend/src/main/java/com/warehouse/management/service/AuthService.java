package com.warehouse.management.service;

import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.dto.AuthLoginResponse;
import com.warehouse.management.dto.AuthPermissionResponse;
import com.warehouse.management.dto.AuthRegisterRequest;
import com.warehouse.management.dto.AuthUserResponse;
import com.warehouse.management.dto.AuthChangePasswordRequest;

public interface AuthService {

    AuthUserResponse register(AuthRegisterRequest request);

    AuthLoginResponse login(AuthLoginRequest request, String requestIp, String userAgent);

    AuthUserResponse getCurrentUser(Long userId);

    AuthPermissionResponse getCurrentPermissions(Long userId);

    void changePassword(Long userId, AuthChangePasswordRequest request);

    void logout(String token);
}
