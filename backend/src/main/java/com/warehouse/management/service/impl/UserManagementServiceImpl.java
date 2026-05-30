package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.UserCreateRequest;
import com.warehouse.management.dto.UserPasswordUpdateRequest;
import com.warehouse.management.dto.UserResponse;
import com.warehouse.management.dto.UserStatusUpdateRequest;
import com.warehouse.management.dto.UserUpdateRequest;
import com.warehouse.management.entity.User;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.service.UserManagementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final String STAFF_ROLE = "STAFF";

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final Set<String> SUPPORTED_ROLES = Set.of(ADMIN_ROLE, STAFF_ROLE);

    private static final Set<String> SUPPORTED_STATUSES = Set.of(ACTIVE_STATUS, DISABLED_STATUS);

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public UserManagementServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserResponse> page(long page, long size, String keyword, String role, String status) {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(User::getUsername, value)
                    .or()
                    .like(User::getEmail, value)
                    .or()
                    .like(User::getPhone, value));
        }
        if (hasText(role)) {
            query.eq(User::getRole, normalizeRole(role));
        }
        if (hasText(status)) {
            query.eq(User::getStatus, normalizeStatus(status));
        }
        query.orderByDesc(User::getCreatedAt);

        IPage<User> result = userMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        ensureUsernameUnique(username, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(hasText(request.role()) ? normalizeRole(request.role()) : STAFF_ROLE);
        user.setStatus(hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS);
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        userMapper.insert(user);

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getExisting(id);
        String username = request.username().trim();
        ensureUsernameUnique(username, id);

        String status = hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS;
        ensureNotDisablingSelf(id, status);

        user.setUsername(username);
        user.setRole(hasText(request.role()) ? normalizeRole(request.role()) : STAFF_ROLE);
        user.setStatus(status);
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        userMapper.updateById(user);

        return toResponse(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long id, UserPasswordUpdateRequest request) {
        User user = getExisting(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        User user = getExisting(id);
        String status = normalizeStatus(request.status());
        ensureNotDisablingSelf(id, status);

        user.setStatus(status);
        userMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        Long currentUserId = CurrentUserContext.getRequired().id();
        if (currentUserId.equals(id)) {
            throw BusinessException.badRequest("Cannot delete yourself");
        }
        userMapper.deleteById(id);
    }

    private User getExisting(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("User does not exist");
        }
        return user;
    }

    private void ensureUsernameUnique(String username, Long excludedId) {
        LambdaQueryWrapper<User> query = Wrappers.<User>lambdaQuery().eq(User::getUsername, username);
        if (excludedId != null) {
            query.ne(User::getId, excludedId);
        }
        if (userMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("Username already exists");
        }
    }

    private void ensureNotDisablingSelf(Long id, String status) {
        Long currentUserId = CurrentUserContext.getRequired().id();
        if (currentUserId.equals(id) && DISABLED_STATUS.equals(status)) {
            throw BusinessException.badRequest("Cannot disable yourself");
        }
    }

    private String normalizeRole(String role) {
        String value = role.trim().toUpperCase();
        if (!SUPPORTED_ROLES.contains(value)) {
            throw BusinessException.badRequest("Role must be ADMIN or STAFF");
        }
        return value;
    }

    private String normalizeStatus(String status) {
        String value = status.trim().toUpperCase();
        if (!SUPPORTED_STATUSES.contains(value)) {
            throw BusinessException.badRequest("Status must be ACTIVE or DISABLED");
        }
        return value;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private long normalizePage(long page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
