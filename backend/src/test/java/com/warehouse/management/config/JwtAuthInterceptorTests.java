package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.entity.User;
import com.warehouse.management.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthInterceptorTests {

    private final JwtProperties jwtProperties = createJwtProperties();
    private final JwtUtil jwtUtil = new JwtUtil(jwtProperties);
    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(jwtUtil);

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

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(user));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(CurrentUserContext.getRequired().id()).isEqualTo(7L);
        assertThat(CurrentUserContext.getRequired().username()).isEqualTo("operator");
        assertThat(CurrentUserContext.getRequired().role()).isEqualTo("STAFF");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(CurrentUserContext.get()).isNull();
    }

    private static JwtProperties createJwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("warehouse-management-system-test-secret-key-2026");
        jwtProperties.setExpirationHours(24);
        return jwtProperties;
    }
}
