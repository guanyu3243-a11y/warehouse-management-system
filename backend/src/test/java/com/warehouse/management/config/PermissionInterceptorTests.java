package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionInterceptorTests {

    private final AuthorizationService authorizationService = mock(AuthorizationService.class);

    private final PermissionInterceptor interceptor = new PermissionInterceptor(authorizationService);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void adminBypassesPermissionCheck() {
        CurrentUserContext.set(new CurrentUser(1L, "admin", "ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/products/10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verifyNoInteractions(authorizationService);
    }

    @Test
    void staffWithoutPermissionIsRejected() {
        CurrentUserContext.set(new CurrentUser(2L, "staff", "STAFF"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authorizationService.hasPermission(2L, "product:create")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);

        verify(authorizationService).hasPermission(2L, "product:create");
    }

    @Test
    void staffWithPermissionIsAllowed() {
        CurrentUserContext.set(new CurrentUser(3L, "staff", "STAFF"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/stock");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authorizationService.hasPermission(3L, "stock:view")).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verify(authorizationService).hasPermission(3L, "stock:view");
    }
}
