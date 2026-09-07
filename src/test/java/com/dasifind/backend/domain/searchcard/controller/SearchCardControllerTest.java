package com.dasifind.backend.domain.searchcard.controller;

import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailAnalysisResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailImageResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailLostLocationResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListItemResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardUpdateResponse;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.service.SearchCardCloseService;
import com.dasifind.backend.domain.searchcard.service.SearchCardCreateService;
import com.dasifind.backend.domain.searchcard.service.SearchCardDetailQueryService;
import com.dasifind.backend.domain.searchcard.service.SearchCardQueryService;
import com.dasifind.backend.domain.searchcard.service.SearchCardUpdateService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private SearchCardQueryService searchCardQueryService;

    @MockitoBean
    private SearchCardDetailQueryService searchCardDetailQueryService;

    @MockitoBean
    private SearchCardUpdateService searchCardUpdateService;

    @MockitoBean
    private SearchCardCloseService searchCardCloseService;

    @Test
    void 추천_후보로_물건을_찾아_수색을_종료한다() throws Exception {
        when(searchCardCloseService.close(eq(7L), eq(12L), any()))
                .thenReturn(new SearchCardCloseResponse(
                        12L,
                        SearchCardStatus.FOUND,
                        LocalDateTime.of(2026, 8, 25, 15, 0)
                ));

        mockMvc.perform(post("/api/v1/search-cards/12/close")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FOUND",
                                  "reason": "FOUND_BY_RECOMMENDATION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.searchCardId").value(12))
                .andExpect(jsonPath("$.result.status").value("FOUND"))
                .andExpect(jsonPath("$.result.closedAt").value("2026-08-25T15:00:00"));

        verify(searchCardCloseService).close(eq(7L), eq(12L), any());
    }

    @Test
    void 종료_상태와_사유_조합이_올바르지_않으면_거절한다() throws Exception {
        when(searchCardCloseService.close(eq(7L), eq(12L), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST));

        mockMvc.perform(post("/api/v1/search-cards/12/close")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED",
                                  "reason": "FOUND_OTHER_WAY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 이미_종료된_수색카드는_다시_종료할_수_없다() throws Exception {
        when(searchCardCloseService.close(eq(7L), eq(12L), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SEARCH_CARD_STATUS));

        mockMvc.perform(post("/api/v1/search-cards/12/close")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED",
                                  "reason": "SEARCH_STOPPED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEARCH4091"));
    }

    @Test
    void 수색_종료는_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/search-cards/12/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED",
                                  "reason": "SEARCH_STOPPED"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 재분석_결과로_활성_수색카드를_수정한다() throws Exception {
        when(searchCardUpdateService.update(eq(7L), eq(12L), any()))
                .thenReturn(new SearchCardUpdateResponse(
                        12L,
                        SearchCardStatus.ACTIVE,
                        false
                ));

        mockMvc.perform(patch("/api/v1/search-cards/12")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.searchCardId").value(12))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.rematchScheduled").value(false));

        verify(searchCardUpdateService).update(eq(7L), eq(12L), any());
    }

    @Test
    void 활성_상태가_아니면_수색카드를_수정할_수_없다() throws Exception {
        when(searchCardUpdateService.update(eq(7L), eq(12L), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SEARCH_CARD_STATUS));

        mockMvc.perform(patch("/api/v1/search-cards/12")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEARCH4091"));
    }

    @Test
    void 수정_요청에_새_분석_ID가_없으면_거절한다() throws Exception {
        String request = validUpdateRequest().replace("\"analysisId\": 902,", "");

        mockMvc.perform(patch("/api/v1/search-cards/12")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @Test
    void 수색카드_수정은_인증이_필요하다() throws Exception {
        mockMvc.perform(patch("/api/v1/search-cards/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 본인의_수색카드_상세를_조회한다() throws Exception {
        SearchCardDetailResponse response = new SearchCardDetailResponse(
                12L,
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY", "BLACK"),
                null,
                "LEATHER",
                "앞면 중앙에 은색 로고가 있어요.",
                List.of(new SearchCardDetailImageResponse(
                        501L,
                        "https://download.example.com/501",
                        SearchCardImageType.REFERENCE
                )),
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                new SearchCardDetailLostLocationResponse(
                        "판교역 스타벅스",
                        "경기도 성남시 분당구 판교역로 166",
                        new BigDecimal("37.3947000"),
                        new BigDecimal("127.1112000"),
                        "카페에서 나올 때까지는 있었어요."
                ),
                new SearchCardDetailAnalysisResponse(
                        List.of("앞면 은색 로고"),
                        "preprocess-v1"
                ),
                SearchCardStatus.ACTIVE,
                LocalDateTime.of(2026, 9, 16, 23, 59, 59),
                0,
                null
        );
        when(searchCardDetailQueryService.getMySearchCard(7L, 12L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/search-cards/12")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(12))
                .andExpect(jsonPath("$.result.category").value("WALLET"))
                .andExpect(jsonPath("$.result.colors[0]").value("NAVY"))
                .andExpect(jsonPath("$.result.brand").value(nullValue()))
                .andExpect(jsonPath("$.result.images[0].id").value(501))
                .andExpect(jsonPath("$.result.images[0].imageType").value("REFERENCE"))
                .andExpect(jsonPath("$.result.lostLocation.placeName")
                        .value("판교역 스타벅스"))
                .andExpect(jsonPath("$.result.analysis.features[0]")
                        .value("앞면 은색 로고"))
                .andExpect(jsonPath("$.result.candidateCount").value(0))
                .andExpect(jsonPath("$.result.bestCandidateScore").value(nullValue()));

        verify(searchCardDetailQueryService).getMySearchCard(7L, 12L);
    }

    @Test
    void 다른_사용자의_수색카드_상세는_조회할_수_없다() throws Exception {
        when(searchCardDetailQueryService.getMySearchCard(7L, 12L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/search-cards/12")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON4031"));
    }

    @Test
    void 존재하지_않는_수색카드_상세는_조회할_수_없다() throws Exception {
        when(searchCardDetailQueryService.getMySearchCard(7L, 999L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/search-cards/999")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON4041"));
    }

    @Test
    void 수색카드_상세_조회는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards/12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 수색카드_ID는_양수여야_한다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards/0")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 내_수색카드를_기본_페이징으로_최근순_조회한다() throws Exception {
        when(searchCardQueryService.getMySearchCards(7L, null, 0, 20))
                .thenReturn(new SearchCardListResponse(
                        List.of(new SearchCardListItemResponse(
                                12L,
                                "남색 카드지갑",
                                SearchCardStatus.ACTIVE,
                                LocalDate.of(2026, 8, 17),
                                "판교역 스타벅스",
                                null,
                                LocalDateTime.of(2026, 9, 16, 23, 59, 59)
                        )),
                        0,
                        20,
                        1,
                        false
                ));

        mockMvc.perform(get("/api/v1/search-cards")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].id").value(12))
                .andExpect(jsonPath("$.result.content[0].itemName").value("남색 카드지갑"))
                .andExpect(jsonPath("$.result.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.content[0].lostDate").value("2026-08-17"))
                .andExpect(jsonPath("$.result.content[0].lostPlaceName").value("판교역 스타벅스"))
                .andExpect(jsonPath("$.result.content[0].bestCandidateScore").value(nullValue()))
                .andExpect(jsonPath("$.result.content[0].searchExpiresAt")
                        .value("2026-09-16T23:59:59"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.hasNext").value(false));

        verify(searchCardQueryService).getMySearchCards(7L, null, 0, 20);
    }

    @Test
    void 상태와_페이지를_지정해_내_수색카드를_조회한다() throws Exception {
        when(searchCardQueryService.getMySearchCards(7L, SearchCardStatus.CLOSED, 1, 10))
                .thenReturn(new SearchCardListResponse(List.of(), 1, 10, 11, false));

        mockMvc.perform(get("/api/v1/search-cards")
                        .param("status", "CLOSED")
                        .param("page", "1")
                        .param("size", "10")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.size").value(10));

        verify(searchCardQueryService)
                .getMySearchCards(7L, SearchCardStatus.CLOSED, 1, 10);
    }

    @Test
    void 존재하지_않는_상태이면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards")
                        .param("status", "UNKNOWN")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 페이지가_음수이면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards")
                        .param("page", "-1")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 페이지_크기가_최대값을_초과하면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards")
                        .param("size", "101")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 수색카드_목록_조회는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/v1/search-cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

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

    private String validUpdateRequest() {
        return """
                {
                  "analysisId": 902,
                  "category": "WALLET",
                  "itemName": "남색 카드지갑",
                  "color": ["NAVY", "BLACK"],
                  "brand": null,
                  "material": "LEATHER",
                  "featureDescription": "오른쪽 아래에 큰 긁힘이 있어요.",
                  "lostDate": "2026-08-17",
                  "lostStartTime": "18:00",
                  "lostEndTime": "21:00",
                  "lostLocation": {
                    "placeName": "판교역 스타벅스",
                    "address": "경기도 성남시 분당구 판교역로 166",
                    "latitude": 37.3947,
                    "longitude": 127.1112,
                    "description": "카페에서 마지막으로 사용했습니다."
                  }
                }
                """;
    }
}
