package com.warehouse.management.service;

import com.warehouse.management.dto.AuthPermissionResponse;

import java.util.Set;

public interface AuthorizationService {

    AuthPermissionResponse getCurrentPermissions(Long userId);

    Set<String> listRoleCodes(Long userId);

    Set<String> listPermissionCodes(Long userId);

    Set<Long> listWarehouseIds(Long userId);

    boolean hasPermission(Long userId, String permissionCode);
}
