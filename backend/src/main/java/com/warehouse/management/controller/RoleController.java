package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.RoleCreateRequest;
import com.warehouse.management.dto.RolePermissionUpdateRequest;
import com.warehouse.management.dto.RoleResponse;
import com.warehouse.management.dto.RoleStatusUpdateRequest;
import com.warehouse.management.dto.RoleUpdateRequest;
import com.warehouse.management.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(roleService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<RoleResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoleStatusUpdateRequest request
    ) {
        return ApiResponse.success(roleService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable Long id) {
        return ApiResponse.success(roleService.getPermissions(id));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<List<PermissionResponse>> updatePermissions(
            @PathVariable Long id,
            @RequestBody RolePermissionUpdateRequest request
    ) {
        return ApiResponse.success(roleService.updatePermissions(id, request));
    }
}
