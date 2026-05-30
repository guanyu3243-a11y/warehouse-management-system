package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PermissionModuleResponse;
import com.warehouse.management.dto.PermissionRequest;
import com.warehouse.management.dto.PermissionResponse;
import com.warehouse.management.dto.PermissionStatusUpdateRequest;
import com.warehouse.management.entity.Permission;
import com.warehouse.management.mapper.PermissionMapper;
import com.warehouse.management.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final Set<String> SUPPORTED_TYPES = Set.of("MENU", "BUTTON", "API");

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<PermissionResponse> list(String keyword, String module, String type, String status) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Permission::getCode, value)
                    .or()
                    .like(Permission::getName, value));
        }
        if (hasText(module)) {
            query.eq(Permission::getModule, module.trim().toUpperCase());
        }
        if (hasText(type)) {
            query.eq(Permission::getType, normalizeType(type));
        }
        if (hasText(status)) {
            query.eq(Permission::getStatus, normalizeStatus(status));
        }
        query.orderByAsc(Permission::getModule).orderByAsc(Permission::getSortOrder);

        return permissionMapper.selectList(query).stream().map(this::toResponse).toList();
    }

    @Override
    public List<PermissionModuleResponse> tree() {
        Map<String, List<PermissionResponse>> grouped = list(null, null, null, ACTIVE_STATUS)
                .stream()
                .collect(Collectors.groupingBy(
                        PermissionResponse::module,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return grouped.entrySet().stream()
                .map(entry -> new PermissionModuleResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        String code = normalizeCode(request.code());
        ensureCodeUnique(code, null);

        Permission permission = new Permission();
        permission.setCode(code);
        applyRequest(permission, request);
        permissionMapper.insert(permission);
        return toResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission permission = getExisting(id);
        String code = normalizeCode(request.code());
        ensureCodeUnique(code, id);

        permission.setCode(code);
        applyRequest(permission, request);
        permissionMapper.updateById(permission);
        return toResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse updateStatus(Long id, PermissionStatusUpdateRequest request) {
        Permission permission = getExisting(id);
        permission.setStatus(normalizeStatus(request.status()));
        permissionMapper.updateById(permission);
        return toResponse(permission);
    }

    private Permission getExisting(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw BusinessException.notFound("Permission does not exist");
        }
        return permission;
    }

    private void ensureCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<Permission> query = Wrappers.<Permission>lambdaQuery().eq(Permission::getCode, code);
        if (excludedId != null) {
            query.ne(Permission::getId, excludedId);
        }
        if (permissionMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("Permission code already exists");
        }
    }

    private void applyRequest(Permission permission, PermissionRequest request) {
        permission.setName(request.name().trim());
        permission.setType(normalizeType(request.type()));
        permission.setModule(request.module().trim().toUpperCase());
        permission.setPath(trimToNull(request.path()));
        permission.setMethod(hasText(request.method()) ? request.method().trim().toUpperCase() : null);
        permission.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        permission.setStatus(hasText(request.status()) ? normalizeStatus(request.status()) : ACTIVE_STATUS);
    }

    private PermissionResponse toResponse(Permission permission) {
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

    private String normalizeCode(String code) {
        return code.trim().toLowerCase();
    }

    private String normalizeType(String type) {
        String value = type.trim().toUpperCase();
        if (!SUPPORTED_TYPES.contains(value)) {
            throw BusinessException.badRequest("Permission type must be MENU, BUTTON or API");
        }
        return value;
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
}
