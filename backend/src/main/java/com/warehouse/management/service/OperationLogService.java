package com.warehouse.management.service;

import com.warehouse.management.dto.OperationLogResponse;
import com.warehouse.management.dto.PageResponse;

import java.time.LocalDateTime;

public interface OperationLogService {

    PageResponse<OperationLogResponse> page(
            long page,
            long size,
            Long userId,
            String module,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    OperationLogResponse getById(Long id);

    void record(
            Long userId,
            String module,
            String action,
            String method,
            String requestUri,
            String requestIp,
            String description
    );
}
