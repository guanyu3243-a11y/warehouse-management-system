package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.dto.LoginLogResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.entity.LoginLog;
import com.warehouse.management.mapper.LoginLogMapper;
import com.warehouse.management.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogServiceImpl.class);

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public PageResponse<LoginLogResponse> page(
            long page,
            long size,
            String username,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        LambdaQueryWrapper<LoginLog> query = Wrappers.lambdaQuery();
        query.like(hasText(username), LoginLog::getUsername, username == null ? null : username.trim());
        query.eq(success != null, LoginLog::getSuccess, success);
        query.ge(startTime != null, LoginLog::getCreatedAt, startTime);
        query.le(endTime != null, LoginLog::getCreatedAt, endTime);
        query.orderByDesc(LoginLog::getCreatedAt);

        IPage<LoginLog> result = loginLogMapper.selectPage(
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
    public void record(Long userId, String username, boolean success, String failureReason, String requestIp, String userAgent) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setSuccess(success);
            loginLog.setFailureReason(failureReason);
            loginLog.setRequestIp(requestIp);
            loginLog.setUserAgent(truncate(userAgent, 255));
            loginLog.setCreatedAt(LocalDateTime.now());
            loginLogMapper.insert(loginLog);
        } catch (DataAccessException exception) {
            log.warn("Failed to write login log", exception);
        }
    }

    @Override
    public long countRecentFailures(String username, LocalDateTime since) {
        if (!hasText(username)) {
            return 0;
        }
        return loginLogMapper.selectCount(
                Wrappers.<LoginLog>lambdaQuery()
                        .eq(LoginLog::getUsername, username.trim())
                        .eq(LoginLog::getSuccess, false)
                        .ge(LoginLog::getCreatedAt, since)
        );
    }

    private LoginLogResponse toResponse(LoginLog loginLog) {
        return new LoginLogResponse(
                loginLog.getId(),
                loginLog.getUserId(),
                loginLog.getUsername(),
                loginLog.getSuccess(),
                loginLog.getFailureReason(),
                loginLog.getRequestIp(),
                loginLog.getUserAgent(),
                loginLog.getCreatedAt()
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
