package com.warehouse.management.dto;

public record OperationLogRecordCommand(
        Long userId,
        String module,
        String action,
        String method,
        String requestUri,
        String requestIp,
        String description,
        String requestBody,
        Integer responseStatus,
        String errorMessage,
        String beforeData,
        String afterData,
        String userAgent
) {
}
