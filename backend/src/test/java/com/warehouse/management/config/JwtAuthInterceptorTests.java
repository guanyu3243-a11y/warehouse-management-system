package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.service.TokenBlacklistService;
import com.warehouse.management.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthInterceptorTests {

    private final JwtProperties jwtProperties = createJwtProperties();
    private final JwtUtil jwtUtil = new JwtUtil(jwtProperties);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(jwtUtil, tokenBlacklistService, userMapper);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void missingTokenIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    void validTokenSetsCurrentUserContextAndAfterCompletionClearsIt() {
        User user = new User();
        user.setId(7L);
        user.setUsername("operator");
        user.setRole("STAFF");
        user.setStatus("ACTIVE");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        String token = jwtUtil.generateToken(user);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(userMapper.selectById(7L)).thenReturn(user);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(CurrentUserContext.getRequired().id()).isEqualTo(7L);
        assertThat(CurrentUserContext.getRequired().username()).isEqualTo("operator");
        assertThat(CurrentUserContext.getRequired().role()).isEqualTo("STAFF");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(CurrentUserContext.get()).isNull();
    }

    @Test
    void disabledUserTokenIsRejected() {
        User user = new User();
        user.setId(8L);
        user.setUsername("disabled");
        user.setRole("STAFF");
        user.setStatus("DISABLED");
        String token = jwtUtil.generateToken(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(userMapper.selectById(8L)).thenReturn(user);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    private static JwtProperties createJwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("warehouse-management-system-test-secret-key-2026");
        jwtProperties.setExpirationHours(24);
        return jwtProperties;
    }
}
