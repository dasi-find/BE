package com.dasifind.backend.global.api;

import com.dasifind.backend.global.error.ErrorCode;

public record ApiResponse<T>(
        boolean isSuccess,
        String code,
        String message,
        T result
) {

    private static final String SUCCESS_CODE = "COMMON2001";
    private static final String SUCCESS_MESSAGE = "요청에 성공하였습니다.";

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, result);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
