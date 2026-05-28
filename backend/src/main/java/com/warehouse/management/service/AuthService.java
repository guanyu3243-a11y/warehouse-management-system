package com.warehouse.management.service;

import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.dto.AuthLoginResponse;
import com.warehouse.management.dto.AuthRegisterRequest;
import com.warehouse.management.dto.AuthUserResponse;

public interface AuthService {

    AuthUserResponse register(AuthRegisterRequest request);

    AuthLoginResponse login(AuthLoginRequest request);

    AuthUserResponse getCurrentUser(Long userId);
}
