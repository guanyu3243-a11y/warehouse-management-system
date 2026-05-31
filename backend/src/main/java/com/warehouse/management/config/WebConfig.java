package com.warehouse.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    private final AdminOnlyInterceptor adminOnlyInterceptor;

    private final PermissionInterceptor permissionInterceptor;

    private final OperationLogInterceptor operationLogInterceptor;

    public WebConfig(
            JwtAuthInterceptor jwtAuthInterceptor,
            AdminOnlyInterceptor adminOnlyInterceptor,
            PermissionInterceptor permissionInterceptor,
            OperationLogInterceptor operationLogInterceptor
    ) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminOnlyInterceptor = adminOnlyInterceptor;
        this.permissionInterceptor = permissionInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/health",
                        "/error"
                );

        registry.addInterceptor(adminOnlyInterceptor)
                .addPathPatterns(
                        "/api/users", "/api/users/**",
                        "/api/roles", "/api/roles/**",
                        "/api/permissions", "/api/permissions/**",
                        "/api/login-logs", "/api/login-logs/**"
                );

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/me",
                        "/api/auth/permissions",
                        "/api/auth/logout",
                        "/api/health",
                        "/error"
                );

        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/health",
                        "/error"
                );
    }
}
