package com.dasifind.backend.domain.searchcard.analysis.controller;

import com.dasifind.backend.domain.searchcard.analysis.dto.request.SearchCardAnalysisRequest;
import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.service.SearchCardAnalysisService;
import com.dasifind.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search-card-analyses")
public class SearchCardAnalysisController {

    private final SearchCardAnalysisService searchCardAnalysisService;

    public SearchCardAnalysisController(SearchCardAnalysisService searchCardAnalysisService) {
        this.searchCardAnalysisService = searchCardAnalysisService;
    }

    @PostMapping
    public ApiResponse<SearchCardAnalysisResponse> analyze(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SearchCardAnalysisRequest request
    ) {
        SearchCardAnalysisResponse response = searchCardAnalysisService.analyze(
                Long.valueOf(jwt.getSubject()),
                request
        );
        return ApiResponse.success(response);
    }
}
