package com.warehouse.management.common;

public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), null);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        String responseMessage = message == null || message.isBlank() ? errorCode.getDefaultMessage() : message;
        return new ApiResponse<>(errorCode.getCode(), responseMessage, null);
    }
}
