package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
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
import com.warehouse.management.service.UserManagementService;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(userManagementService.page(page, size, keyword, role, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.getById(id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userManagementService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userManagementService.update(id, request));
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userManagementService.updatePassword(id, request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ApiResponse.success(userManagementService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userManagementService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/roles")
    public ApiResponse<List<RoleResponse>> getRoles(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.getRoles(id));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<List<RoleResponse>> updateRoles(
            @PathVariable Long id,
            @RequestBody UserRoleUpdateRequest request
    ) {
        return ApiResponse.success(userManagementService.updateRoles(id, request));
    }

    @GetMapping("/{id}/warehouses")
    public ApiResponse<List<WarehouseResponse>> getWarehouses(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.getWarehouses(id));
    }

    @PutMapping("/{id}/warehouses")
    public ApiResponse<List<WarehouseResponse>> updateWarehouses(
            @PathVariable Long id,
            @RequestBody UserWarehousePermissionUpdateRequest request
    ) {
        return ApiResponse.success(userManagementService.updateWarehouses(id, request));
    }
}
