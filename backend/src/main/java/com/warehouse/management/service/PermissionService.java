package com.warehouse.management.service;

import com.warehouse.management.dto.PermissionModuleResponse;
import com.warehouse.management.dto.PermissionRequest;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.PermissionStatusUpdateRequest;

import java.util.List;

public interface PermissionService {

    List<PermissionResponse> list(String keyword, String module, String type, String status);

    List<PermissionModuleResponse> tree();

    PermissionResponse create(PermissionRequest request);

    PermissionResponse update(Long id, PermissionRequest request);

    PermissionResponse updateStatus(Long id, PermissionStatusUpdateRequest request);
}
