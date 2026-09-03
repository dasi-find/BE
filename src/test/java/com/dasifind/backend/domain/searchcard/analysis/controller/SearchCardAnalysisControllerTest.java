package com.dasifind.backend.domain.searchcard.analysis.controller;

import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.service.SearchCardAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchCardAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchCardAnalysisService searchCardAnalysisService;

    @Test
    void 사진_없이_AI_분석을_요청한다() throws Exception {
        when(searchCardAnalysisService.analyze(eq(7L), any())).thenReturn(
                new SearchCardAnalysisResponse(
                        801L,
                        "WALLET",
                        "CARD_WALLET",
                        List.of("NAVY", "BLACK"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("앞면 은색 로고"),
                        "preprocess-v1"
                )
        );

        mockMvc.perform(post("/api/v1/search-card-analyses")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.analysisId").value(801))
                .andExpect(jsonPath("$.result.itemName").value("CARD_WALLET"))
                .andExpect(jsonPath("$.result.colors[0]").value("NAVY"))
                .andExpect(jsonPath("$.result.modelVersion").value("preprocess-v1"));

        verify(searchCardAnalysisService).analyze(eq(7L), any());
    }

    @Test
    void 필수값이_누락되면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/search-card-analyses")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @Test
    void 이미지_ID_배열은_최대_5개까지_허용한다() throws Exception {
        String request = validRequest().replace(
                "\"imageIds\": []",
                "\"imageIds\": [1, 2, 3, 4, 5, 6]"
        );

        mockMvc.perform(post("/api/v1/search-card-analyses")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void AI_분석_요청은_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/search-card-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    private String validRequest() {
        return """
                {
                  "category": "WALLET",
                  "itemName": "남색 카드지갑",
                  "color": ["NAVY"],
                  "brand": null,
                  "featureDescription": "앞면 중앙에 은색 로고가 있어요.",
                  "imageIds": [],
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
