package com.dasifind.backend.global.api;

import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void result를_포함한_성공_응답을_생성한다() {
        ApiResponse<String> response = ApiResponse.success("result");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("COMMON2001");
        assertThat(response.message()).isEqualTo("요청에 성공하였습니다.");
        assertThat(response.result()).isEqualTo("result");
    }

    @Test
    void result가_null인_실패_응답을_생성한다() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.code()).isEqualTo("COMMON4041");
        assertThat(response.message()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
        assertThat(response.result()).isNull();
    }

    @Test
    void 명세에_정의된_필드명으로_JSON을_생성한다() {
        String json = jsonMapper.writeValueAsString(ApiResponse.success("result"));

        assertThat(json).contains(
                "\"isSuccess\":true",
                "\"code\":\"COMMON2001\"",
                "\"message\":\"요청에 성공하였습니다.\"",
                "\"result\":\"result\""
        );
    }
}
