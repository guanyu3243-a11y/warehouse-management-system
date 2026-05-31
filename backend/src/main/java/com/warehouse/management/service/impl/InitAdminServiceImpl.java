package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.config.InitAdminProperties;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.service.InitAdminService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitAdminServiceImpl implements InitAdminService {

    private static final Logger log = LoggerFactory.getLogger(InitAdminServiceImpl.class);

    private static final String ADMIN_ROLE = "ADMIN";

    private final InitAdminProperties properties;

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    public InitAdminServiceImpl(
            InitAdminProperties properties,
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void initializeIfNecessary() {
        Long userCount = userMapper.selectCount(Wrappers.lambdaQuery(User.class));
        if (userCount != null && userCount > 0) {
            log.info("Initial admin creation skipped because users table is not empty");
            return;
        }

        String username = requireText(properties.getUsername(), "INIT_ADMIN_USERNAME");
        String rawPassword = requireText(properties.getPassword(), "INIT_ADMIN_PASSWORD");

        Role adminRole = roleMapper.selectOne(
                Wrappers.lambdaQuery(Role.class)
                        .eq(Role::getCode, ADMIN_ROLE)
                        .last("LIMIT 1")
        );
        if (adminRole == null) {
            throw new IllegalStateException("ADMIN role does not exist. Check Flyway RBAC migration.");
        }

        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(ADMIN_ROLE);
        admin.setStatus("ACTIVE");
        admin.setEmail(blankToNull(properties.getEmail()));
        admin.setPhone(blankToNull(properties.getPhone()));
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userMapper.insert(admin);

        UserRole userRole = new UserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setCreatedAt(now);
        userRole.setUpdatedAt(now);
        userRoleMapper.insert(userRole);

        log.info("Initial admin user '{}' created", username);
    }

    private String requireText(String value, String envName) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalStateException(envName + " must be set when INIT_ADMIN_ENABLED=true");
        }
        return text;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
