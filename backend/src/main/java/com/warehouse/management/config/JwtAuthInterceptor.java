package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.service.TokenBlacklistService;
import com.warehouse.management.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final JwtUtil jwtUtil;

    private final TokenBlacklistService tokenBlacklistService;

    private final UserMapper userMapper;

    public JwtAuthInterceptor(
            JwtUtil jwtUtil,
            TokenBlacklistService tokenBlacklistService,
            UserMapper userMapper
    ) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw BusinessException.unauthorized("Missing authorization token");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw BusinessException.unauthorized("Token has been logged out");
        }

        Claims claims = jwtUtil.parseToken(token);
        Object userIdClaim = claims.get("userId");
        if (!(userIdClaim instanceof Number userIdNumber)) {
            throw BusinessException.unauthorized("Token is invalid or expired");
        }

        Long userId = userIdNumber.longValue();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("User does not exist");
        }
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw BusinessException.forbidden("User account is disabled");
        }

        CurrentUserContext.set(new CurrentUser(userId, user.getUsername(), user.getRole()));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        CurrentUserContext.clear();
    }
}
