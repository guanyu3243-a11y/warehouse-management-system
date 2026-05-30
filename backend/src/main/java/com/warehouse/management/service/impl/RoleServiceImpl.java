package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.RoleCreateRequest;
import com.warehouse.management.dto.RolePermissionUpdateRequest;
import com.warehouse.management.dto.RoleResponse;
import com.warehouse.management.dto.RoleStatusUpdateRequest;
import com.warehouse.management.dto.RoleUpdateRequest;
import com.warehouse.management.entity.Permission;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.RolePermission;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.mapper.PermissionMapper;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.RolePermissionMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of("ADMIN", "MANAGER", "STAFF", "VIEWER");

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final UserRoleMapper userRoleMapper;

    public RoleServiceImpl(
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            UserRoleMapper userRoleMapper
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResponse<RoleResponse> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Role> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Role::getCode, value)
                    .or()
                    .like(Role::getName, value));
        }
        if (hasText(status)) {
            query.eq(Role::getStatus, normalizeStatus(status));
        }
        query.orderByAsc(Role::getCode);

        IPage<Role> result = roleMapper.selectPage(
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
    public RoleResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        String code = normalizeCode(request.code());
        ensureCodeUnique(code);

        Role role = new Role();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        role.setStatus(hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS);
        roleMapper.insert(role);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = getExisting(id);
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        role.setStatus(hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS);
        roleMapper.updateById(role);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse updateStatus(Long id, RoleStatusUpdateRequest request) {
        Role role = getExisting(id);
        role.setStatus(normalizeStatus(request.status()));
        roleMapper.updateById(role);
        return toResponse(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = getExisting(id);
        if (BUILT_IN_ROLE_CODES.contains(role.getCode())) {
            throw BusinessException.badRequest("Built-in role cannot be deleted");
        }
        Long userCount = userRoleMapper.selectCount(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getRoleId, id)
        );
        if (userCount > 0) {
            throw BusinessException.badRequest("Role is already assigned to users");
        }
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    public List<PermissionResponse> getPermissions(Long id) {
        getExisting(id);
        Set<Long> permissionIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id)
                )
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(
                        Wrappers.<Permission>lambdaQuery()
                                .in(Permission::getId, permissionIds)
                                .orderByAsc(Permission::getSortOrder)
                )
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<PermissionResponse> updatePermissions(Long id, RolePermissionUpdateRequest request) {
        getExisting(id);
        Set<Long> permissionIds = sanitizeIds(request.permissionIds());
        if (!permissionIds.isEmpty()) {
            List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
            if (permissions.size() != permissionIds.size()) {
                throw BusinessException.badRequest("Permission list contains invalid permission id");
            }
        }

        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id));
        permissionIds.forEach(permissionId -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(id);
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        });
        return getPermissions(id);
    }

    private Role getExisting(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.notFound("Role does not exist");
        }
        return role;
    }

    private void ensureCodeUnique(String code) {
        if (roleMapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code)) > 0) {
            throw BusinessException.badRequest("Role code already exists");
        }
    }

    private RoleResponse toResponse(Role role) {
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

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getType(),
                permission.getModule(),
                permission.getPath(),
                permission.getMethod(),
                permission.getSortOrder(),
                permission.getStatus(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }

    private Set<Long> sanitizeIds(List<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeStatus(String status) {
        String value = status.trim().toUpperCase();
        if (!Set.of(ACTIVE_STATUS, DISABLED_STATUS).contains(value)) {
            throw BusinessException.badRequest("Status must be ACTIVE or DISABLED");
        }
        return value;
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
