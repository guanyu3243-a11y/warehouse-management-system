package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.service.AuthorizationService;
import com.warehouse.management.service.LoginLogService;
import com.warehouse.management.service.OperationLogService;
import com.warehouse.management.service.TokenBlacklistService;
import com.warehouse.management.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplSecurityTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoginLogService loginLogService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userMapper,
                roleMapper,
                userRoleMapper,
                passwordEncoder,
                jwtUtil,
                operationLogService,
                authorizationService,
                loginLogService,
                tokenBlacklistService
        );
    }

    @Test
    void loginIsTemporarilyBlockedAfterTooManyRecentFailures() {
        when(loginLogService.countRecentFailures(eq("admin"), any(LocalDateTime.class))).thenReturn(5L);

        assertThatThrownBy(() -> authService.login(
                new AuthLoginRequest("admin", "bad-password"),
                "127.0.0.1",
                "JUnit"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);

        verify(loginLogService).record(
                null,
                "admin",
                false,
                "Too many failed login attempts",
                "127.0.0.1",
                "JUnit"
        );
        verifyNoInteractions(userMapper, jwtUtil, operationLogService);
    }
}
