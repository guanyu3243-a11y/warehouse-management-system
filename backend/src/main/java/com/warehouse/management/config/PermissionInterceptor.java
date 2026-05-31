package com.warehouse.management.config;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.service.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AuthorizationService authorizationService;

    public PermissionInterceptor(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String permissionCode = resolvePermissionCode(request.getMethod(), request.getRequestURI());
        if (permissionCode == null) {
            return true;
        }

        CurrentUser currentUser = CurrentUserContext.getRequired();
        if (ADMIN_ROLE.equals(currentUser.role())) {
            return true;
        }

        if (!authorizationService.hasPermission(currentUser.id(), permissionCode)) {
            throw BusinessException.forbidden("Permission is required: " + permissionCode);
        }
        return true;
    }

    private String resolvePermissionCode(String method, String uri) {
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/health")) {
            return null;
        }
        if (uri.startsWith("/api/dashboard")) {
            return "dashboard:view";
        }
        if (uri.startsWith("/api/categories")) {
            return actionPermission("category", method, uri);
        }
        if (uri.startsWith("/api/products")) {
            return actionPermission("product", method, uri);
        }
        if (uri.startsWith("/api/warehouses")) {
            return actionPermission("warehouse", method, uri);
        }
        if (uri.startsWith("/api/suppliers")) {
            return actionPermission("supplier", method, uri);
        }
        if (uri.startsWith("/api/users")) {
            return actionPermission("user", method, uri);
        }
        if (uri.startsWith("/api/roles")) {
            return actionPermission("role", method, uri);
        }
        if (uri.startsWith("/api/permissions")) {
            if ("GET".equalsIgnoreCase(method)) {
                return "permission:view";
            }
            return "permission:update";
        }
        if (uri.startsWith("/api/stock-in")) {
            return stockDocumentPermission("stock-in", method, uri);
        }
        if (uri.startsWith("/api/stock-out")) {
            return stockDocumentPermission("stock-out", method, uri);
        }
        if (uri.startsWith("/api/inventory-adjustments")) {
            return stockDocumentPermission("inventory-adjustment", method, uri);
        }
        if (uri.startsWith("/api/stock-takes")) {
            return stockTakePermission(method, uri);
        }
        if (uri.startsWith("/api/stock-movements")) {
            return "stock-movement:view";
        }
        if (uri.startsWith("/api/stock/low")) {
            return "stock:low:view";
        }
        if (uri.startsWith("/api/stock")) {
            return "stock:view";
        }
        if (uri.startsWith("/api/operation-logs")) {
            return "operation-log:view";
        }
        return null;
    }

    private String actionPermission(String module, String method, String uri) {
        if ("GET".equalsIgnoreCase(method)) {
            return module + ":view";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return module + ":create";
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return module + ":update";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return module + ":delete";
        }
        return null;
    }

    private String stockDocumentPermission(String module, String method, String uri) {
        if ("GET".equalsIgnoreCase(method)) {
            return module + ":view";
        }
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/confirm")) {
            return module + ":confirm";
        }
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/cancel")) {
            return module + ":cancel";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return module + ":create";
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return module + ":update";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return module + ":delete";
        }
        return null;
    }

    private String stockTakePermission(String method, String uri) {
        if ("GET".equalsIgnoreCase(method) && uri.endsWith("/export")) {
            return "stock-take:export";
        }
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/import")) {
            return "stock-take:import";
        }
        return stockDocumentPermission("stock-take", method, uri);
    }
}
