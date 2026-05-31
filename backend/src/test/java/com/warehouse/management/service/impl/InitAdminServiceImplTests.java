package com.warehouse.management.service.impl;

import com.warehouse.management.config.InitAdminProperties;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitAdminServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private InitAdminProperties properties;

    private InitAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new InitAdminProperties();
        service = new InitAdminServiceImpl(properties, userMapper, roleMapper, userRoleMapper, passwordEncoder);
    }

    @Test
    void initializeSkipsWhenUsersAlreadyExist() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        service.initializeIfNecessary();

        verify(userMapper, never()).insert(any(User.class));
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void initializeCreatesAdminAndBindsAdminRoleWhenUsersTableIsEmpty() {
        properties.setUsername("admin");
        properties.setPassword("Admin123456");

        Role adminRole = new Role();
        adminRole.setId(7L);
        adminRole.setCode("ADMIN");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectOne(any())).thenReturn(adminRole);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        });

        service.initializeIfNecessary();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getRole()).isEqualTo("ADMIN");
        assertThat(savedUser.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedUser.getPassword()).isNotEqualTo("Admin123456");
        assertThat(passwordEncoder.matches("Admin123456", savedUser.getPassword())).isTrue();

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        UserRole savedUserRole = userRoleCaptor.getValue();
        assertThat(savedUserRole.getUserId()).isEqualTo(100L);
        assertThat(savedUserRole.getRoleId()).isEqualTo(7L);
    }

    @Test
    void initializeFailsWithoutPassword() {
        properties.setUsername("admin");
        when(userMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.initializeIfNecessary())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INIT_ADMIN_PASSWORD");
    }
}
