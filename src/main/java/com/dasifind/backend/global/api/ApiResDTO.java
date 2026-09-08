package com.dasifind.backend.global.api;

import com.dasifind.backend.global.error.ErrorCode;

public record ApiResDTO<T>(
        boolean isSuccess,
        String code,
        String message,
        T result
) {

    private static final String SUCCESS_CODE = "COMMON2001";
    private static final String SUCCESS_MESSAGE = "요청에 성공하였습니다.";

    public static <T> ApiResDTO<T> success(T result) {
        return new ApiResDTO<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, result);
    }

    public static ApiResDTO<Void> success() {
        return success(null);
    }

    public static ApiResDTO<Void> failure(ErrorCode errorCode) {
        return new ApiResDTO<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
