package com.dasifind.backend.domain.candidate.service;

import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CandidateSummaryQueryService {
    private final CandidateRepository candidates;

    public CandidateSummaryQueryService(CandidateRepository candidates) {
        this.candidates = candidates;
    }

    public Map<Long, Summary> summarize(Long userId, Collection<Long> cardIds) {
        if (cardIds.isEmpty()) return Map.of();
        return candidates.summarizeByCardIds(userId, cardIds).stream()
                .collect(Collectors.toUnmodifiableMap(row -> row.getSearchCardId(),
                        row -> new Summary(row.getCandidateCount(), row.getBestCandidateScore())));
    }

    public record Summary(long candidateCount, BigDecimal bestCandidateScore) {
        public static final Summary EMPTY = new Summary(0, null);
    }
}
