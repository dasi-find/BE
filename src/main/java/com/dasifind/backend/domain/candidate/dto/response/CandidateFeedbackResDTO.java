package com.dasifind.backend.domain.candidate.dto.response;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;

public record CandidateFeedbackResDTO(Long candidateId, CandidateFeedback feedback, boolean isExcluded) {
    public static CandidateFeedbackResDTO from(Candidate candidate) {
        return new CandidateFeedbackResDTO(candidate.getId(), candidate.getFeedback(), candidate.isExcluded());
    }
}
