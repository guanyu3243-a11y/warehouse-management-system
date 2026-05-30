package com.warehouse.management.service.impl;

import com.warehouse.management.dto.UserCreateRequest;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTests {

    @Mock
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserManagementServiceImpl userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void createEncryptsPasswordWithBCrypt() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

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
}
