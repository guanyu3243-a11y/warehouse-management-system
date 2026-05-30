package com.warehouse.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    private final AdminOnlyInterceptor adminOnlyInterceptor;

    private final OperationLogInterceptor operationLogInterceptor;

    public WebConfig(
            JwtAuthInterceptor jwtAuthInterceptor,
            AdminOnlyInterceptor adminOnlyInterceptor,
            OperationLogInterceptor operationLogInterceptor
    ) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminOnlyInterceptor = adminOnlyInterceptor;
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
                .addPathPatterns("/api/users", "/api/users/**");

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
