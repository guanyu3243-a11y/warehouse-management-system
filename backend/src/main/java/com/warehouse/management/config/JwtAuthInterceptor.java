package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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

        Claims claims = jwtUtil.parseToken(authorization.substring(BEARER_PREFIX.length()));
        Object userIdClaim = claims.get("userId");
        if (!(userIdClaim instanceof Number userIdNumber)) {
            throw BusinessException.unauthorized("Token is invalid or expired");
        }

        Long userId = userIdNumber.longValue();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        CurrentUserContext.set(new CurrentUser(userId, username, role));
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
