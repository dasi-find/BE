package com.dasifind.backend.domain.candidate.controller;

import com.dasifind.backend.domain.candidate.dto.request.CandidateFeedbackReqDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateDetailResDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateListResDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateFeedbackResDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateViewResDTO;
import com.dasifind.backend.domain.candidate.service.CandidateQueryService;
import com.dasifind.backend.domain.candidate.service.CandidateInteractionService;
import com.dasifind.backend.global.api.ApiResDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@Validated
public class CandidateController {
    private final CandidateQueryService queries;
    private final CandidateInteractionService interactions;

    public CandidateController(CandidateQueryService queries, CandidateInteractionService interactions) {
        this.queries = queries;
        this.interactions = interactions;
    }

    @GetMapping("/api/v1/search-cards/{searchCardId}/candidates")
    public ApiResDTO<CandidateListResDTO> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long searchCardId,
            @RequestParam(required = false) @DecimalMin("0") @DecimalMax("100") BigDecimal minScore,
            @RequestParam(defaultValue = "false") boolean includeExcluded,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size) {
        return ApiResDTO.success(queries.list(Long.valueOf(jwt.getSubject()), searchCardId,
                minScore, includeExcluded, page, size));
    }

    @GetMapping("/api/v1/candidates/{candidateId}")
    public ApiResDTO<CandidateDetailResDTO> detail(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long candidateId) {
        return ApiResDTO.success(queries.detail(Long.valueOf(jwt.getSubject()), candidateId));
    }

    @PostMapping("/api/v1/candidates/{candidateId}/view")
    public ApiResDTO<CandidateViewResDTO> view(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long candidateId) {
        return ApiResDTO.success(interactions.view(Long.valueOf(jwt.getSubject()), candidateId));
    }

    @PutMapping("/api/v1/candidates/{candidateId}/feedback")
    public ApiResDTO<CandidateFeedbackResDTO> feedback(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long candidateId,
            @Valid @RequestBody CandidateFeedbackReqDTO request) {
        return ApiResDTO.success(interactions.feedback(Long.valueOf(jwt.getSubject()), candidateId, request.feedback()));
    }

    @DeleteMapping("/api/v1/candidates/{candidateId}/feedback")
    public ApiResDTO<CandidateFeedbackResDTO> clearFeedback(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long candidateId) {
        return ApiResDTO.success(interactions.clearFeedback(Long.valueOf(jwt.getSubject()), candidateId));
    }
}
