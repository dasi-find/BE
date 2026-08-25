package com.dasifind.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON4001", "요청값이 올바르지 않습니다."),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "COMMON4004", "필수 입력값이 누락되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON4011", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON4031", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON4041", "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "COMMON4091", "동일한 요청이 중복되었습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "COMMON4291", "요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON5001", "서버 내부 오류가 발생했습니다."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH4011", "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4012", "인증 토큰이 유효하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH4091", "이미 가입된 이메일입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH4001", "이메일 인증번호가 일치하지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.GONE, "AUTH4101", "이메일 인증번호 또는 인증 토큰이 만료되었습니다."),

    INVALID_SEARCH_CARD_STATUS(HttpStatus.CONFLICT, "SEARCH4091", "현재 수색카드 상태에서는 요청을 처리할 수 없습니다."),

    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "IMAGE4131", "이미지 파일 크기 제한을 초과했습니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE4151", "지원하지 않는 이미지 형식입니다."),

    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "AI5021", "AI 분석 처리에 실패했습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI5031", "AI 서비스를 일시적으로 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
