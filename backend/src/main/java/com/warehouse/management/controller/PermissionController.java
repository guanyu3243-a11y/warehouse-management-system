package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PermissionModuleResponse;
import com.warehouse.management.dto.PermissionRequest;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.PermissionStatusUpdateRequest;
import com.warehouse.management.service.PermissionService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(permissionService.list(keyword, module, type, status));
    }

    @GetMapping("/tree")
    public ApiResponse<List<PermissionModuleResponse>> tree() {
        return ApiResponse.success(permissionService.tree());
    }

    @PostMapping
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequest request
    ) {
        return ApiResponse.success(permissionService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<PermissionResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PermissionStatusUpdateRequest request
    ) {
        return ApiResponse.success(permissionService.updateStatus(id, request));
    }
}
