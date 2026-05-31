package com.warehouse.management.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuditRequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (shouldWrap(request)) {
            filterChain.doFilter(new ContentCachingRequestWrapper(request), response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldWrap(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();
        return uri != null
                && uri.startsWith("/api/")
                && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))
                && (contentType == null || !contentType.toLowerCase().startsWith("multipart/"));
    }
}
