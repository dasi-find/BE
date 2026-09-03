package com.dasifind.backend.domain.searchcard.analysis.service;

import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchCardAnalysisQueryService {

    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final UserRepository userRepository;

    public SearchCardAnalysisQueryService(
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            UserRepository userRepository
    ) {
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.userRepository = userRepository;
    }

    public SearchCardAnalysisResponse get(Long userId, Long analysisId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        SearchCardAnalysis analysis = searchCardAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!analysis.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return SearchCardAnalysisResponse.from(analysis);
    }
}
