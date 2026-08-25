package com.dasifind.backend.global.error;

import com.dasifind.backend.global.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void 비즈니스_예외를_정의된_오류_응답으로_변환한다() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void 올바르지_않은_요청을_공통_400_응답으로_변환한다() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidRequest(
                new HttpMessageNotReadableException("invalid", new MockHttpInputMessage(new byte[0]))
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 필수값_검증에_실패하면_필수_입력값_누락으로_응답한다() {
        MethodArgumentNotValidException exception = validationException("NotBlank");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure(ErrorCode.REQUIRED_FIELD_MISSING));
    }

    @Test
    void 형식_검증에_실패하면_올바르지_않은_요청으로_응답한다() {
        MethodArgumentNotValidException exception = validationException("Size");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 예상하지_못한_예외의_내부_내용을_응답에_노출하지_않는다() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpectedException(
                new RuntimeException("sensitive detail")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody().message()).doesNotContain("sensitive detail");
    }

    private MethodArgumentNotValidException validationException(String validationCode) {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError(
                "request",
                "field",
                null,
                false,
                new String[]{validationCode},
                null,
                "invalid"
        ));
        return new MethodArgumentNotValidException(null, bindingResult);
    }
}
