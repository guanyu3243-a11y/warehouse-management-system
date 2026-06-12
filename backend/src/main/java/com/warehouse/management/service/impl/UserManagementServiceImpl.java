package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.RoleResponse;
import com.warehouse.management.dto.UserCreateRequest;
import com.warehouse.management.dto.UserPasswordUpdateRequest;
import com.warehouse.management.dto.UserResponse;
import com.warehouse.management.dto.UserRoleUpdateRequest;
import com.warehouse.management.dto.UserStatusUpdateRequest;
import com.warehouse.management.dto.UserUpdateRequest;
import com.warehouse.management.dto.UserWarehousePermissionUpdateRequest;
import com.warehouse.management.dto.WarehouseResponse;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.entity.UserWarehousePermission;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.mapper.UserWarehousePermissionMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.UserManagementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final String STAFF_ROLE = "STAFF";

    private static final String MANAGER_ROLE = "MANAGER";

    private static final String VIEWER_ROLE = "VIEWER";

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final Set<String> SUPPORTED_ROLES = Set.of(ADMIN_ROLE, MANAGER_ROLE, STAFF_ROLE, VIEWER_ROLE);

    private static final Set<String> SUPPORTED_STATUSES = Set.of(ACTIVE_STATUS, DISABLED_STATUS);

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final UserWarehousePermissionMapper userWarehousePermissionMapper;

    private final WarehouseMapper warehouseMapper;

    private final PasswordEncoder passwordEncoder;

    public UserManagementServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            UserWarehousePermissionMapper userWarehousePermissionMapper,
            WarehouseMapper warehouseMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userWarehousePermissionMapper = userWarehousePermissionMapper;
        this.warehouseMapper = warehouseMapper;
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
        syncPrimaryUserRole(user.getId(), user.getRole());

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getExisting(id);
        String username = request.username().trim();
        ensureUsernameUnique(username, id);

        String status = hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS;
        ensureCanSetStatus(user, status);

        user.setUsername(username);
        user.setRole(hasText(request.role()) ? normalizeRole(request.role()) : STAFF_ROLE);
        user.setStatus(status);
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        userMapper.updateById(user);
        syncPrimaryUserRole(user.getId(), user.getRole());

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
        ensureCanSetStatus(user, status);

        user.setStatus(status);
        userMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getExisting(id);
        ensureCanSetStatus(user, DISABLED_STATUS);
        if (DISABLED_STATUS.equals(user.getStatus())) {
            return;
        }
        user.setStatus(DISABLED_STATUS);
        userMapper.updateById(user);
    }

    @Override
    public List<RoleResponse> getRoles(Long id) {
        getExisting(id);
        Set<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, id)
                )
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream().map(this::toRoleResponse).toList();
    }

    @Override
    @Transactional
    public List<RoleResponse> updateRoles(Long id, UserRoleUpdateRequest request) {
        User user = getExisting(id);
        Set<Long> roleIds = sanitizeIds(request.roleIds());
        if (roleIds.isEmpty()) {
            throw BusinessException.badRequest("At least one role is required");
        }

        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BusinessException.badRequest("Role list contains invalid role id");
        }
        if (roles.stream().anyMatch(role -> DISABLED_STATUS.equals(role.getStatus()))) {
            throw BusinessException.badRequest("Disabled role cannot be assigned");
        }

        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, id));
        roles.forEach(role -> {
            UserRole userRole = new UserRole();
            userRole.setUserId(id);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        });

        String primaryRole = roles.stream()
                .map(Role::getCode)
                .filter(SUPPORTED_ROLES::contains)
                .findFirst()
                .orElse(STAFF_ROLE);
        user.setRole(primaryRole);
        userMapper.updateById(user);

        return getRoles(id);
    }

    @Override
    public List<WarehouseResponse> getWarehouses(Long id) {
        getExisting(id);
        Set<Long> warehouseIds = userWarehousePermissionMapper.selectList(
                        Wrappers.<UserWarehousePermission>lambdaQuery()
                                .eq(UserWarehousePermission::getUserId, id)
                )
                .stream()
                .map(UserWarehousePermission::getWarehouseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (warehouseIds.isEmpty()) {
            return List.of();
        }
        return warehouseMapper.selectBatchIds(warehouseIds).stream().map(this::toWarehouseResponse).toList();
    }

    @Override
    @Transactional
    public List<WarehouseResponse> updateWarehouses(Long id, UserWarehousePermissionUpdateRequest request) {
        getExisting(id);
        Set<Long> warehouseIds = sanitizeIds(request.warehouseIds());
        if (!warehouseIds.isEmpty()) {
            List<Warehouse> warehouses = warehouseMapper.selectBatchIds(warehouseIds);
            if (warehouses.size() != warehouseIds.size()) {
                throw BusinessException.badRequest("Warehouse list contains invalid warehouse id");
            }
        }

        userWarehousePermissionMapper.delete(
                Wrappers.<UserWarehousePermission>lambdaQuery()
                        .eq(UserWarehousePermission::getUserId, id)
        );
        warehouseIds.forEach(warehouseId -> {
            UserWarehousePermission permission = new UserWarehousePermission();
            permission.setUserId(id);
            permission.setWarehouseId(warehouseId);
            userWarehousePermissionMapper.insert(permission);
        });

        return getWarehouses(id);
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

    private void ensureCanSetStatus(User user, String status) {
        if (!DISABLED_STATUS.equals(status) || DISABLED_STATUS.equals(user.getStatus())) {
            return;
        }

        Long currentUserId = CurrentUserContext.getRequired().id();
        if (currentUserId.equals(user.getId())) {
            throw BusinessException.badRequest("Cannot disable yourself");
        }

        if (ADMIN_ROLE.equals(user.getRole())) {
            long activeAdminCount = userMapper.selectCount(
                    Wrappers.<User>lambdaQuery()
                            .eq(User::getRole, ADMIN_ROLE)
                            .eq(User::getStatus, ACTIVE_STATUS)
            );
            if (activeAdminCount <= 1) {
                throw BusinessException.badRequest("Cannot disable the last active ADMIN user");
            }
        }
    }

    private String normalizeRole(String role) {
        String value = role.trim().toUpperCase();
        if (!SUPPORTED_ROLES.contains(value)) {
            throw BusinessException.badRequest("Role must be ADMIN, MANAGER, STAFF or VIEWER");
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

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getContactName(),
                warehouse.getContactPhone(),
                warehouse.getStatus(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }

    private void syncPrimaryUserRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(
                Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode)
        );
        if (role == null) {
            return;
        }
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private Set<Long> sanitizeIds(List<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
