package com.zhihuiji.backend.api.common;

public record ApiResponse<T>(
    int code,
    String message,
    T data,
    long timestamp
) {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_METHOD_NOT_ALLOWED = 405;
    public static final int CODE_UNPROCESSABLE_ENTITY = 422;
    public static final int CODE_INTERNAL_ERROR = 500;
    public static final int CODE_SERVICE_UNAVAILABLE = 503;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(CODE_SUCCESS, "success", data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}

