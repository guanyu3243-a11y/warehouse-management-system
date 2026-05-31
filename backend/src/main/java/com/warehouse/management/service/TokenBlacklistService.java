package com.warehouse.management.service;

import java.time.LocalDateTime;

public interface TokenBlacklistService {

    void blacklist(String token, Long userId, String username, LocalDateTime expiresAt);

    boolean isBlacklisted(String token);
}
