package com.dasifind.backend.domain.searchcard.controller;

import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCreateRequest;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResponse;
import com.dasifind.backend.domain.searchcard.service.SearchCardCreateService;
import com.dasifind.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search-cards")
public class SearchCardController {

    private final SearchCardCreateService searchCardCreateService;

    public SearchCardController(SearchCardCreateService searchCardCreateService) {
        this.searchCardCreateService = searchCardCreateService;
    }

    @PostMapping
    public ApiResponse<SearchCardCreateResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SearchCardCreateRequest request
    ) {
        SearchCardCreateResponse response = searchCardCreateService.create(
                Long.valueOf(jwt.getSubject()),
                request
        );
        return ApiResponse.success(response);
    }
}
