package com.dasifind.backend.domain.candidate.repository;

import java.math.BigDecimal;

public interface CandidateSummaryProjection {
    Long getSearchCardId();
    long getCandidateCount();
    BigDecimal getBestCandidateScore();
}
