package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.UserCreateRequest;
import com.warehouse.management.dto.UserPasswordUpdateRequest;
import com.warehouse.management.dto.UserResponse;
import com.warehouse.management.dto.UserStatusUpdateRequest;
import com.warehouse.management.dto.UserUpdateRequest;

public interface UserManagementService {

    PageResponse<UserResponse> page(long page, long size, String keyword, String role, String status);

    UserResponse getById(Long id);

    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    void updatePassword(Long id, UserPasswordUpdateRequest request);

    UserResponse updateStatus(Long id, UserStatusUpdateRequest request);

    void delete(Long id);
}
