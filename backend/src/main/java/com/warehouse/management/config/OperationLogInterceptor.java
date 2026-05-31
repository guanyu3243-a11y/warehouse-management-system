package com.warehouse.management.config;

import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.OperationLogRecordCommand;
import com.warehouse.management.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogService operationLogService;

    public OperationLogInterceptor(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (!shouldRecord(request)) {
            return;
        }

        CurrentUser currentUser = CurrentUserContext.get();
        Long userId = currentUser == null ? null : currentUser.id();
        String requestUri = request.getRequestURI();
        String action = inferAction(request.getMethod(), requestUri);
        String module = inferModule(requestUri);
        String description = "HTTP " + response.getStatus();
        String sanitizedBody = requestBody(request);

        operationLogService.record(new OperationLogRecordCommand(
                userId,
                module,
                action,
                request.getMethod(),
                requestUri,
                request.getRemoteAddr(),
                description,
                sanitizedBody,
                response.getStatus(),
                ex == null ? null : ex.getMessage(),
                "DELETE".equalsIgnoreCase(request.getMethod()) ? sanitizedBody : null,
                ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()))
                        ? sanitizedBody
                        : null,
                request.getHeader("User-Agent")
        ));
    }

    private boolean shouldRecord(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))
                && uri.startsWith("/api/")
                && !uri.startsWith("/api/operation-logs");
    }

    private String inferAction(String method, String uri) {
        if (uri.endsWith("/confirm")) {
            return "CONFIRM";
        }
        if (uri.endsWith("/cancel")) {
            return "CANCEL";
        }
        if (uri.endsWith("/logout")) {
            return "LOGOUT";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "CREATE";
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return "UPDATE";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "DELETE";
        }
        return method.toUpperCase();
    }

    private String inferModule(String uri) {
        String[] parts = uri.split("/");
        if (parts.length < 3) {
            return "UNKNOWN";
        }
        return parts[2].replace("-", "_").toUpperCase();
    }

    private String requestBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        Charset charset = wrapper.getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(wrapper.getCharacterEncoding());
        return maskSensitiveFields(new String(content, charset));
    }

    private String maskSensitiveFields(String value) {
        return value
                .replaceAll("(?i)(\"(?:oldPassword|newPassword|password)\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3")
                .replaceAll("(?i)((?:oldPassword|newPassword|password)=)([^&\\s]*)", "$1******");
    }
}
