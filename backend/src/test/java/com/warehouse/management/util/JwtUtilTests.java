package com.warehouse.management.util;

import com.warehouse.management.config.JwtProperties;
import com.warehouse.management.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTests {

    @Test
    void generatedTokenContainsUserClaims() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("warehouse-management-system-test-secret-key-2026");
        jwtProperties.setExpirationHours(24);

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole("ADMIN");
        user.setPassword("encoded-password");
        user.setEmail("admin@example.com");
        user.setPhone("13800000000");

        JwtUtil jwtUtil = new JwtUtil(jwtProperties);
        String token = jwtUtil.generateToken(user);
        Claims claims = jwtUtil.parseToken(token);

        assertThat(claims.getSubject()).isNull();
        assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(1L);
        assertThat(claims.get("username", String.class)).isEqualTo("admin");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims).doesNotContainKeys("email", "phone", "password", "realName");
        assertThat(claims.keySet()).containsExactlyInAnyOrder("userId", "username", "role", "iat", "exp");
    }
}
