package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.AuthLoginRequest;
import com.warehouse.management.dto.AuthLoginResponse;
import com.warehouse.management.dto.AuthRegisterRequest;
import com.warehouse.management.dto.AuthUserResponse;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.service.AuthService;
import com.warehouse.management.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "STAFF";

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
        return toUserResponse(user);
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username().trim())
        );
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.unauthorized("Invalid username or password");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            throw BusinessException.forbidden("User account is disabled");
        }

        String token = jwtUtil.generateToken(user);
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
        return toUserResponse(user);
    }

    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
