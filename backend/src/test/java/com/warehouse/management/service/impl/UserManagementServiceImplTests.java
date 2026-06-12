package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.UserCreateRequest;
import com.warehouse.management.dto.UserStatusUpdateRequest;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.mapper.UserWarehousePermissionMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserWarehousePermissionMapper userWarehousePermissionMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserManagementServiceImpl userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementServiceImpl(
                userMapper,
                roleMapper,
                userRoleMapper,
                userWarehousePermissionMapper,
                warehouseMapper,
                passwordEncoder
        );
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void createEncryptsPasswordWithBCrypt() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });
        Role role = new Role();
        role.setId(1L);
        role.setCode("ADMIN");
        role.setName("系统管理员");
        role.setStatus("ACTIVE");
        when(roleMapper.selectOne(any())).thenReturn(role);

        userManagementService.create(new UserCreateRequest(
                "new-admin",
                "123456",
                "ADMIN",
                null,
                "admin@example.com",
                "13800000000"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getRole()).isEqualTo("ADMIN");
        assertThat(savedUser.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void deleteDisablesOrdinaryUserWithoutDeletingUserOrRoleHistory() {
        CurrentUserContext.set(new CurrentUser(1L, "admin", "ADMIN"));
        User user = user(2L, "staff", "STAFF", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(user);

        userManagementService.delete(2L);

        assertThat(user.getStatus()).isEqualTo("DISABLED");
        verify(userMapper).updateById(user);
        verify(userMapper, never()).deleteById(2L);
        verifyNoInteractions(userRoleMapper, userWarehousePermissionMapper);
    }

    @Test
    void updateStatusCannotDisableCurrentUser() {
        CurrentUserContext.set(new CurrentUser(1L, "admin", "ADMIN"));
        User user = user(1L, "admin", "ADMIN", "ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> userManagementService.updateStatus(
                1L,
                new UserStatusUpdateRequest("DISABLED")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot disable yourself");

        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void deleteCannotDisableLastActiveAdmin() {
        CurrentUserContext.set(new CurrentUser(1L, "admin", "ADMIN"));
        User user = user(2L, "backup-admin", "ADMIN", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userManagementService.delete(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot disable the last active ADMIN user");

        verify(userMapper, never()).updateById(any(User.class));
        verify(userMapper, never()).deleteById(2L);
    }

    private User user(Long id, String username, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
