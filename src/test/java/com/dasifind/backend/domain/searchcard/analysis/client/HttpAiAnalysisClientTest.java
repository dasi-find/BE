package com.dasifind.backend.domain.searchcard.analysis.client;

import com.dasifind.backend.domain.searchcard.analysis.config.AiAnalysisProperties;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAiAnalysisClientTest {

    @Test
    void AI_서버에_요청하고_구조화된_결과를_받는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiAnalysisClient client = new HttpAiAnalysisClient(
                builder.build(),
                properties("http://ai.example")
        );
        server.expect(once(), requestTo("http://ai.example/internal/v1/search-card-analyses"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "category": "WALLET",
                          "itemName": "CARD_WALLET",
                          "colors": ["NAVY"],
                          "brand": null,
                          "materials": ["LEATHER"],
                          "ocrText": null,
                          "features": ["앞면 은색 로고"],
                          "modelVersion": "preprocess-v1"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiAnalysisClientResponse response = client.analyze(request());

        assertThat(response.itemName()).isEqualTo("CARD_WALLET");
        assertThat(response.colors()).containsExactly("NAVY");
        server.verify();
    }

    @Test
    void AI_서버_주소가_없으면_서비스_이용_불가로_처리한다() {
        HttpAiAnalysisClient client = new HttpAiAnalysisClient(
                RestClient.create(),
                properties("")
        );

        assertError(client, ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void AI_서버가_503을_반환하면_서비스_이용_불가로_처리한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiAnalysisClient client = new HttpAiAnalysisClient(
                builder.build(),
                properties("http://ai.example")
        );
        server.expect(requestTo("http://ai.example/internal/v1/search-card-analyses"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        assertError(client, ErrorCode.AI_SERVICE_UNAVAILABLE);
        server.verify();
    }

    @Test
    void AI_서버가_그_외_오류를_반환하면_분석_실패로_처리한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiAnalysisClient client = new HttpAiAnalysisClient(
                builder.build(),
                properties("http://ai.example")
        );
        server.expect(requestTo("http://ai.example/internal/v1/search-card-analyses"))
                .andRespond(withServerError());

        assertError(client, ErrorCode.AI_ANALYSIS_FAILED);
        server.verify();
    }

    private void assertError(HttpAiAnalysisClient client, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> client.analyze(request()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private AiAnalysisProperties properties(String baseUrl) {
        return new AiAnalysisProperties(
                baseUrl,
                "/internal/v1/search-card-analyses",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30)
        );
    }

    private AiAnalysisClientRequest request() {
        return new AiAnalysisClientRequest(
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY"),
                null,
                "앞면 중앙에 은색 로고가 있어요.",
                List.of(),
                LocalDate.of(2026, 8, 17),
                null,
                null,
                new AiAnalysisClientRequest.Location(
                        "판교역",
                        "경기도 성남시 분당구 판교역로 166",
                        new BigDecimal("37.3947"),
                        new BigDecimal("127.1112"),
                        null
                )
        );
    }
}
