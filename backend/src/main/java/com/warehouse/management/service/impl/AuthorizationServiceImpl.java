package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.dto.AuthPermissionResponse;
import com.warehouse.management.entity.Permission;
import com.warehouse.management.entity.Role;
import com.warehouse.management.entity.RolePermission;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.UserRole;
import com.warehouse.management.entity.UserWarehousePermission;
import com.warehouse.management.mapper.PermissionMapper;
import com.warehouse.management.mapper.RoleMapper;
import com.warehouse.management.mapper.RolePermissionMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.UserRoleMapper;
import com.warehouse.management.mapper.UserWarehousePermissionMapper;
import com.warehouse.management.service.AuthorizationService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final UserRoleMapper userRoleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final UserWarehousePermissionMapper userWarehousePermissionMapper;

    public AuthorizationServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            UserRoleMapper userRoleMapper,
            RolePermissionMapper rolePermissionMapper,
            UserWarehousePermissionMapper userWarehousePermissionMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userWarehousePermissionMapper = userWarehousePermissionMapper;
    }

    @Override
    public AuthPermissionResponse getCurrentPermissions(Long userId) {
        return new AuthPermissionResponse(
                listRoleCodes(userId).stream().toList(),
                listPermissionCodes(userId).stream().toList(),
                listWarehouseIds(userId).stream().toList()
        );
    }

    @Override
    public Set<String> listRoleCodes(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Set.of();
        }

        Set<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId)
                )
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Role> roles = roleIds.isEmpty()
                ? fallbackRoles(user.getRole())
                : roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> ACTIVE_STATUS.equals(role.getStatus()))
                .sorted(Comparator.comparing(Role::getCode))
                .toList();

        return roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<String> listPermissionCodes(Long userId) {
        Set<String> roleCodes = listRoleCodes(userId);
        if (roleCodes.isEmpty()) {
            return Set.of();
        }

        if (roleCodes.contains(ADMIN_ROLE)) {
            return permissionMapper.selectList(
                            Wrappers.<Permission>lambdaQuery()
                                    .eq(Permission::getStatus, ACTIVE_STATUS)
                                    .orderByAsc(Permission::getSortOrder)
                    )
                    .stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        List<Role> roles = roleMapper.selectList(
                Wrappers.<Role>lambdaQuery()
                        .in(Role::getCode, roleCodes)
                        .eq(Role::getStatus, ACTIVE_STATUS)
        );
        if (roles.isEmpty()) {
            return Set.of();
        }

        Set<Long> permissionIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery()
                                .in(RolePermission::getRoleId, roles.stream().map(Role::getId).toList())
                )
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return Set.of();
        }

        return permissionMapper.selectList(
                        Wrappers.<Permission>lambdaQuery()
                                .in(Permission::getId, permissionIds)
                                .eq(Permission::getStatus, ACTIVE_STATUS)
                                .orderByAsc(Permission::getSortOrder)
                )
                .stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<Long> listWarehouseIds(Long userId) {
        return userWarehousePermissionMapper.selectList(
                        Wrappers.<UserWarehousePermission>lambdaQuery()
                                .eq(UserWarehousePermission::getUserId, userId)
                )
                .stream()
                .map(UserWarehousePermission::getWarehouseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        return listPermissionCodes(userId).contains(permissionCode);
    }

    private List<Role> fallbackRoles(String roleCode) {
        if (!hasText(roleCode)) {
            return List.of();
        }
        Role role = roleMapper.selectOne(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getCode, roleCode.trim().toUpperCase())
                        .eq(Role::getStatus, ACTIVE_STATUS)
        );
        return role == null ? List.of() : List.of(role);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
