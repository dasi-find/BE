package com.dasifind.backend.domain.searchcard.analysis.controller;

import com.dasifind.backend.domain.searchcard.analysis.dto.request.SearchCardAnalysisReqDTO;
import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResDTO;
import com.dasifind.backend.domain.searchcard.analysis.service.SearchCardAnalysisQueryService;
import com.dasifind.backend.domain.searchcard.analysis.service.SearchCardAnalysisService;
import com.dasifind.backend.global.api.ApiResDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search-card-analyses")
public class SearchCardAnalysisController {

    private final SearchCardAnalysisService searchCardAnalysisService;
    private final SearchCardAnalysisQueryService searchCardAnalysisQueryService;

    public SearchCardAnalysisController(
            SearchCardAnalysisService searchCardAnalysisService,
            SearchCardAnalysisQueryService searchCardAnalysisQueryService
    ) {
        this.searchCardAnalysisService = searchCardAnalysisService;
        this.searchCardAnalysisQueryService = searchCardAnalysisQueryService;
    }

    @PostMapping
    public ApiResDTO<SearchCardAnalysisResDTO> analyze(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SearchCardAnalysisReqDTO request
    ) {
        SearchCardAnalysisResDTO response = searchCardAnalysisService.analyze(
                Long.valueOf(jwt.getSubject()),
                request
        );
        return ApiResDTO.success(response);
    }

    @GetMapping("/{analysisId}")
    public ApiResDTO<SearchCardAnalysisResDTO> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long analysisId
    ) {
        SearchCardAnalysisResDTO response = searchCardAnalysisQueryService.get(
                Long.valueOf(jwt.getSubject()),
                analysisId
        );
        return ApiResDTO.success(response);
    }
}
