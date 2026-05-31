package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.entity.TokenBlacklist;
import com.warehouse.management.mapper.TokenBlacklistMapper;
import com.warehouse.management.service.TokenBlacklistService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistMapper tokenBlacklistMapper;

    public TokenBlacklistServiceImpl(TokenBlacklistMapper tokenBlacklistMapper) {
        this.tokenBlacklistMapper = tokenBlacklistMapper;
    }

    @Override
    public void blacklist(String token, Long userId, String username, LocalDateTime expiresAt) {
        if (!hasText(token) || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            return;
        }

        TokenBlacklist tokenBlacklist = new TokenBlacklist();
        tokenBlacklist.setTokenHash(hash(token));
        tokenBlacklist.setUserId(userId);
        tokenBlacklist.setUsername(username);
        tokenBlacklist.setExpiresAt(expiresAt);
        tokenBlacklist.setBlacklistedAt(LocalDateTime.now());
        try {
            tokenBlacklistMapper.insert(tokenBlacklist);
        } catch (DuplicateKeyException ignored) {
            // Logging out twice should be idempotent.
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (!hasText(token)) {
            return false;
        }
        Long count = tokenBlacklistMapper.selectCount(
                Wrappers.<TokenBlacklist>lambdaQuery()
                        .eq(TokenBlacklist::getTokenHash, hash(token))
                        .gt(TokenBlacklist::getExpiresAt, LocalDateTime.now())
        );
        return count > 0;
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
