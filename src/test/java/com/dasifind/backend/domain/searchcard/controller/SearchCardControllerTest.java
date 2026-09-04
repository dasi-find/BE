package com.dasifind.backend.domain.searchcard.controller;

import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResponse;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.service.SearchCardCreateService;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchCardCreateService searchCardCreateService;

    @Test
    void 수색카드를_생성하고_30일_수색을_시작한다() throws Exception {
        when(searchCardCreateService.create(eq(7L), any())).thenReturn(
                new SearchCardCreateResponse(
                        12L,
                        SearchCardStatus.ACTIVE,
                        LocalDateTime.of(2026, 9, 16, 23, 59, 59),
                        0
                )
        );

        mockMvc.perform(post("/api/v1/search-cards")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.searchCardId").value(12))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.searchExpiresAt").value("2026-09-16T23:59:59"))
                .andExpect(jsonPath("$.result.initialCandidateCount").value(0));

        verify(searchCardCreateService).create(eq(7L), any());
    }

    @Test
    void 필수값이_누락되면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/search-cards")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @Test
    void 위도_범위를_벗어나면_잘못된_요청으로_응답한다() throws Exception {
        String request = validRequest().replace("37.3947", "91.0");

        mockMvc.perform(post("/api/v1/search-cards")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 이미_사용한_분석_결과이면_중복_요청으로_응답한다() throws Exception {
        doThrow(new BusinessException(ErrorCode.DUPLICATE_REQUEST))
                .when(searchCardCreateService).create(eq(7L), any());

        mockMvc.perform(post("/api/v1/search-cards")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON4091"));
    }

    @Test
    void 수색카드_생성은_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/search-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    private String validRequest() {
        return """
                {
                  "analysisId": 801,
                  "category": "WALLET",
                  "itemName": "남색 카드지갑",
                  "color": ["NAVY", "BLACK"],
                  "brand": null,
                  "material": "LEATHER",
                  "featureDescription": "앞면 중앙에 은색 로고가 있어요.",
                  "imageIds": [501],
                  "lostDate": "2026-08-17",
                  "lostStartTime": "18:00",
                  "lostEndTime": "20:00",
                  "lostLocation": {
                    "placeName": "판교역",
                    "address": "경기도 성남시 분당구 판교역로 166",
                    "latitude": 37.3947,
                    "longitude": 127.1112,
                    "description": null
                  }
                }
                """;
    }
}
