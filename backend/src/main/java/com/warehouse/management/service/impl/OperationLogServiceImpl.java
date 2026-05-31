package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.OperationLogResponse;
import com.warehouse.management.dto.OperationLogRecordCommand;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.entity.OperationLog;
import com.warehouse.management.mapper.OperationLogMapper;
import com.warehouse.management.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageResponse<OperationLogResponse> page(
            long page,
            long size,
            Long userId,
            String module,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        LambdaQueryWrapper<OperationLog> query = Wrappers.lambdaQuery();
        query.eq(userId != null, OperationLog::getUserId, userId);
        query.eq(hasText(module), OperationLog::getModule, module == null ? null : module.trim());
        query.eq(hasText(action), OperationLog::getAction, action == null ? null : action.trim());
        query.ge(startTime != null, OperationLog::getCreatedAt, startTime);
        query.le(endTime != null, OperationLog::getCreatedAt, endTime);
        query.orderByDesc(OperationLog::getCreatedAt);

        IPage<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public OperationLogResponse getById(Long id) {
        OperationLog operationLog = operationLogMapper.selectById(id);
        if (operationLog == null) {
            throw BusinessException.notFound("Operation log does not exist");
        }
        return toResponse(operationLog);
    }

    @Override
    public void record(
            Long userId,
            String module,
            String action,
            String method,
            String requestUri,
            String requestIp,
            String description
    ) {
        record(new OperationLogRecordCommand(
                userId,
                module,
                action,
                method,
                requestUri,
                requestIp,
                description,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Override
    public void record(OperationLogRecordCommand command) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(command.userId());
            operationLog.setModule(command.module());
            operationLog.setAction(command.action());
            operationLog.setMethod(command.method());
            operationLog.setRequestUri(command.requestUri());
            operationLog.setRequestIp(command.requestIp());
            operationLog.setDescription(command.description());
            operationLog.setRequestBody(truncate(command.requestBody(), 4000));
            operationLog.setResponseStatus(command.responseStatus());
            operationLog.setErrorMessage(truncate(command.errorMessage(), 500));
            operationLog.setBeforeData(truncate(command.beforeData(), 4000));
            operationLog.setAfterData(truncate(command.afterData(), 4000));
            operationLog.setUserAgent(truncate(command.userAgent(), 255));
            operationLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(operationLog);
        } catch (DataAccessException exception) {
            log.warn("Failed to write operation log", exception);
        }
    }

    private OperationLogResponse toResponse(OperationLog operationLog) {
        return new OperationLogResponse(
                operationLog.getId(),
                operationLog.getUserId(),
                operationLog.getModule(),
                operationLog.getAction(),
                operationLog.getMethod(),
                operationLog.getRequestUri(),
                operationLog.getRequestIp(),
                operationLog.getDescription(),
                operationLog.getRequestBody(),
                operationLog.getResponseStatus(),
                operationLog.getErrorMessage(),
                operationLog.getBeforeData(),
                operationLog.getAfterData(),
                operationLog.getUserAgent(),
                operationLog.getCreatedAt()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long normalizePage(long page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
