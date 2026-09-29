package com.dasifind.backend.domain.candidate.service;

import com.dasifind.backend.domain.candidate.dto.response.CandidateDetailResDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateListResDTO;
import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
public class CandidateQueryService {
    private final CandidateRepository candidates;
    private final SearchCardRepository cards;
    private final UserRepository users;

    public CandidateQueryService(CandidateRepository candidates, SearchCardRepository cards, UserRepository users) {
        this.candidates = candidates;
        this.cards = cards;
        this.users = users;
    }

    public CandidateListResDTO list(Long userId, Long cardId, BigDecimal minScore,
                                    boolean includeExcluded, int page, int size) {
        validateUser(userId);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE
                || (minScore != null && (minScore.signum() < 0 || minScore.compareTo(BigDecimal.valueOf(100)) > 0))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var card = cards.findById(cardId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!card.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        var result = candidates.findVisiblePage(cardId, minScore, includeExcluded, PageRequest.of(page, size));
        var content = new ArrayList<CandidateListResDTO.CandidateListItemResDTO>();
        // One rank query for the first included row; excluded rows never consume a rank.
        Long nextRank = null;
        for (Candidate candidate : result.getContent()) {
            Long rank = null;
            if (!candidate.isExcluded()) {
                if (nextRank == null) nextRank = rankOf(candidate);
                rank = nextRank++;
            }
            content.add(CandidateListResDTO.CandidateListItemResDTO.from(candidate, rank));
        }
        return new CandidateListResDTO(content, page, size, result.getTotalElements(), result.hasNext());
    }

    public CandidateDetailResDTO detail(Long userId, Long candidateId) {
        validateUser(userId);
        Candidate candidate = candidates.findWithItemsById(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateAccess(userId, candidate);
        return CandidateDetailResDTO.from(candidate, rankOf(candidate));
    }

    private Long rankOf(Candidate candidate) {
        if (candidate.isExcluded() || candidate.getTotalScore() == null) return null;
        return candidates.countAhead(candidate.getSearchCard().getId(), candidate.getTotalScore(),
                candidate.getPoliceItem().getFoundDate(), candidate.getId()) + 1;
    }

    private void validateUser(Long userId) {
        if (!users.existsById(userId)) throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    static void validateAccess(Long userId, Candidate candidate) {
        if (!candidate.getSearchCard().getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!candidate.isCurrentAssessment()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
