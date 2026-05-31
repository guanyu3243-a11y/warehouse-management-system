package com.warehouse.management.controller;

import com.warehouse.management.common.GlobalExceptionHandler;
import com.warehouse.management.config.AdminOnlyInterceptor;
import com.warehouse.management.config.JwtAuthInterceptor;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.UserResponse;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.service.TokenBlacklistService;
import com.warehouse.management.service.UserManagementService;
import com.warehouse.management.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerAuthorizationTests {

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserMapper userMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userManagementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(
                        new JwtAuthInterceptor(jwtUtil, tokenBlacklistService, userMapper),
                        new AdminOnlyInterceptor()
                )
                .build();
    }

    @Test
    void staffCannotAccessUserManagementApi() throws Exception {
        mockJwt("staff-token", "STAFF");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer staff-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(userManagementService);
    }

    @Test
    void adminCanAccessUserManagementApi() throws Exception {
        mockJwt("admin-token", "ADMIN");
        when(userManagementService.page(1, 10, null, null, null))
                .thenReturn(new PageResponse<UserResponse>(List.of(), 0, 1, 10));

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(userManagementService).page(1, 10, null, null, null);
    }

    private void mockJwt(String token, String role) {
        Claims claims = mock(Claims.class);
        when(claims.get("userId")).thenReturn(1L);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(role);
        user.setStatus("ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(user);
    }
}
