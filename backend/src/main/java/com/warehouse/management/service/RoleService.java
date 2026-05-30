package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.RoleCreateRequest;
import com.warehouse.management.dto.RolePermissionUpdateRequest;
import com.warehouse.management.dto.RoleResponse;
import com.warehouse.management.dto.RoleStatusUpdateRequest;
import com.warehouse.management.dto.RoleUpdateRequest;

import java.util.List;

public interface RoleService {

    PageResponse<RoleResponse> page(long page, long size, String keyword, String status);

    RoleResponse getById(Long id);

    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(Long id, RoleUpdateRequest request);

    RoleResponse updateStatus(Long id, RoleStatusUpdateRequest request);

    void delete(Long id);

    List<PermissionResponse> getPermissions(Long id);

    List<PermissionResponse> updatePermissions(Long id, RolePermissionUpdateRequest request);
}
