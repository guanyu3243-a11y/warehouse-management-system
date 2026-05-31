package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.AuthChangePasswordRequest;
import com.warehouse.management.dto.AuthPermissionResponse;
import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.dto.AuthLoginResponse;
import com.warehouse.management.dto.AuthRegisterRequest;
import com.warehouse.management.dto.AuthUserResponse;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.service.AuthService;
import com.warehouse.management.service.AuthorizationService;
import com.warehouse.management.service.LoginLogService;
import com.warehouse.management.service.OperationLogService;
import com.warehouse.management.service.TokenBlacklistService;
import com.warehouse.management.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "STAFF";

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private static final int LOGIN_LOCK_MINUTES = 15;

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final OperationLogService operationLogService;

    private final AuthorizationService authorizationService;

    private final LoginLogService loginLogService;

    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            OperationLogService operationLogService,
            AuthorizationService authorizationService,
            LoginLogService loginLogService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.operationLogService = operationLogService;
        this.authorizationService = authorizationService;
        this.loginLogService = loginLogService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public AuthUserResponse register(AuthRegisterRequest request) {
        String username = request.username().trim();
        Long existingCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        if (existingCount > 0) {
            throw BusinessException.badRequest("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        user.setRole(DEFAULT_ROLE);
        user.setStatus(ACTIVE_STATUS);
        userMapper.insert(user);
        syncDefaultRole(user.getId());
        operationLogService.record(
                user.getId(),
                "AUTH",
                "REGISTER",
                "POST",
                "/api/auth/register",
                null,
                "User registered"
        );
        return toUserResponse(user);
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request, String requestIp, String userAgent) {
        String username = request.username().trim();
        LocalDateTime lockWindowStart = LocalDateTime.now().minusMinutes(LOGIN_LOCK_MINUTES);
        if (loginLogService.countRecentFailures(username, lockWindowStart) >= MAX_FAILED_LOGIN_ATTEMPTS) {
            loginLogService.record(
                    null,
                    username,
                    false,
                    "Too many failed login attempts",
                    requestIp,
                    userAgent
            );
            throw BusinessException.forbidden("Too many failed login attempts. Please try again later");
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginLogService.record(
                    user == null ? null : user.getId(),
                    username,
                    false,
                    "Invalid username or password",
                    requestIp,
                    userAgent
            );
            throw BusinessException.unauthorized("Invalid username or password");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            loginLogService.record(
                    user.getId(),
                    username,
                    false,
                    "User account is disabled",
                    requestIp,
                    userAgent
            );
            throw BusinessException.forbidden("User account is disabled");
        }

        String token = jwtUtil.generateToken(user);
        loginLogService.record(user.getId(), username, true, null, requestIp, userAgent);
        operationLogService.record(
                user.getId(),
                "AUTH",
                "LOGIN",
                "POST",
                "/api/auth/login",
                null,
                "User logged in"
        );
        return new AuthLoginResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationSeconds(),
                toUserResponse(user)
        );
    }

    @Override
    public AuthUserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User does not exist");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            throw BusinessException.forbidden("User account is disabled");
        }
        return toUserResponse(user);
    }

    @Override
    public AuthPermissionResponse getCurrentPermissions(Long userId) {
        return authorizationService.getCurrentPermissions(userId);
    }

    @Override
    public void changePassword(Long userId, AuthChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User does not exist");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            throw BusinessException.forbidden("User account is disabled");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("Old password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw BusinessException.badRequest("New password must be different from old password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        operationLogService.record(
                user.getId(),
                "AUTH",
                "CHANGE_PASSWORD",
                "PUT",
                "/api/auth/password",
                null,
                "User changed password"
        );
    }

    @Override
    public void logout(String token) {
        if (token == null) {
            return;
        }
        Claims claims = jwtUtil.parseToken(token);
        Object userIdClaim = claims.get("userId");
        Long userId = userIdClaim instanceof Number userIdNumber ? userIdNumber.longValue() : null;
        String username = claims.get("username", String.class);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(),
                ZoneId.systemDefault()
        );
        tokenBlacklistService.blacklist(token, userId, username, expiresAt);
    }

    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }

    private void syncDefaultRole(Long userId) {
        Role role = roleMapper.selectOne(
                Wrappers.<Role>lambdaQuery().eq(Role::getCode, DEFAULT_ROLE)
        );
        if (role == null) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
