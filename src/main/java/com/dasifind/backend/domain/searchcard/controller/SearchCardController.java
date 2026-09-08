package com.dasifind.backend.domain.searchcard.controller;

import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCloseReqDTO;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCreateReqDTO;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardUpdateReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailResDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListResDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardUpdateResDTO;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.service.SearchCardCloseService;
import com.dasifind.backend.domain.searchcard.service.SearchCardCreateService;
import com.dasifind.backend.domain.searchcard.service.SearchCardDetailQueryService;
import com.dasifind.backend.domain.searchcard.service.SearchCardDeleteService;
import com.dasifind.backend.domain.searchcard.service.SearchCardQueryService;
import com.dasifind.backend.domain.searchcard.service.SearchCardUpdateService;
import com.dasifind.backend.global.api.ApiResDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/search-cards")
public class SearchCardController {

    private final SearchCardCreateService searchCardCreateService;
    private final SearchCardQueryService searchCardQueryService;
    private final SearchCardDetailQueryService searchCardDetailQueryService;
    private final SearchCardUpdateService searchCardUpdateService;
    private final SearchCardCloseService searchCardCloseService;
    private final SearchCardDeleteService searchCardDeleteService;

    public SearchCardController(
            SearchCardCreateService searchCardCreateService,
            SearchCardQueryService searchCardQueryService,
            SearchCardDetailQueryService searchCardDetailQueryService,
            SearchCardUpdateService searchCardUpdateService,
            SearchCardCloseService searchCardCloseService,
            SearchCardDeleteService searchCardDeleteService
    ) {
        this.searchCardCreateService = searchCardCreateService;
        this.searchCardQueryService = searchCardQueryService;
        this.searchCardDetailQueryService = searchCardDetailQueryService;
        this.searchCardUpdateService = searchCardUpdateService;
        this.searchCardCloseService = searchCardCloseService;
        this.searchCardDeleteService = searchCardDeleteService;
    }

    @PostMapping
    public ApiResDTO<SearchCardCreateResDTO> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SearchCardCreateReqDTO request
    ) {
        SearchCardCreateResDTO response = searchCardCreateService.create(
                Long.valueOf(jwt.getSubject()),
                request
        );
        return ApiResDTO.success(response);
    }

    @GetMapping
    public ApiResDTO<SearchCardListResDTO> getMySearchCards(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) SearchCardStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        SearchCardListResDTO response = searchCardQueryService.getMySearchCards(
                Long.valueOf(jwt.getSubject()),
                status,
                page,
                size
        );
        return ApiResDTO.success(response);
    }

    @GetMapping("/{searchCardId}")
    public ApiResDTO<SearchCardDetailResDTO> getMySearchCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(1) Long searchCardId
    ) {
        SearchCardDetailResDTO response = searchCardDetailQueryService.getMySearchCard(
                Long.valueOf(jwt.getSubject()),
                searchCardId
        );
        return ApiResDTO.success(response);
    }

    @PatchMapping("/{searchCardId}")
    public ApiResDTO<SearchCardUpdateResDTO> updateMySearchCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(1) Long searchCardId,
            @Valid @RequestBody SearchCardUpdateReqDTO request
    ) {
        SearchCardUpdateResDTO response = searchCardUpdateService.update(
                Long.valueOf(jwt.getSubject()),
                searchCardId,
                request
        );
        return ApiResDTO.success(response);
    }

    @PostMapping("/{searchCardId}/close")
    public ApiResDTO<SearchCardCloseResDTO> closeMySearchCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(1) Long searchCardId,
            @Valid @RequestBody SearchCardCloseReqDTO request
    ) {
        SearchCardCloseResDTO response = searchCardCloseService.close(
                Long.valueOf(jwt.getSubject()),
                searchCardId,
                request
        );
        return ApiResDTO.success(response);
    }

    @DeleteMapping("/{searchCardId}")
    public ApiResDTO<Void> deleteMySearchCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Min(1) Long searchCardId
    ) {
        searchCardDeleteService.delete(Long.valueOf(jwt.getSubject()), searchCardId);
        return ApiResDTO.success();
    }
}
