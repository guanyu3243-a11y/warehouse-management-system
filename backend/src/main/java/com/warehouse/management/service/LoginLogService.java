package com.warehouse.management.service;

import com.warehouse.management.dto.LoginLogResponse;
import com.warehouse.management.dto.PageResponse;

import java.time.LocalDateTime;

public interface LoginLogService {

    PageResponse<LoginLogResponse> page(
            long page,
            long size,
            String username,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    void record(Long userId, String username, boolean success, String failureReason, String requestIp, String userAgent);

    long countRecentFailures(String username, LocalDateTime since);
}
