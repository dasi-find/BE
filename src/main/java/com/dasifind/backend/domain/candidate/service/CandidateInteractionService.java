package com.dasifind.backend.domain.candidate.service;

import com.dasifind.backend.domain.candidate.dto.response.CandidateFeedbackResDTO;
import com.dasifind.backend.domain.candidate.dto.response.CandidateViewResDTO;
import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class CandidateInteractionService {
    private final CandidateRepository candidates;
    private final UserRepository users;

    public CandidateInteractionService(CandidateRepository candidates, UserRepository users) {
        this.candidates = candidates;
        this.users = users;
    }

    public CandidateViewResDTO view(Long userId, Long candidateId) {
        Candidate candidate = ownedForUpdate(userId, candidateId);
        candidate.markViewed(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return new CandidateViewResDTO(candidate.getId(), candidate.getViewedAt());
    }

    public CandidateFeedbackResDTO feedback(Long userId, Long candidateId, CandidateFeedback feedback) {
        if (feedback == null) throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        Candidate candidate = ownedForUpdate(userId, candidateId);
        candidate.updateFeedback(feedback);
        return CandidateFeedbackResDTO.from(candidate);
    }

    public CandidateFeedbackResDTO clearFeedback(Long userId, Long candidateId) {
        Candidate candidate = ownedForUpdate(userId, candidateId);
        candidate.clearFeedback();
        return CandidateFeedbackResDTO.from(candidate);
    }

    private Candidate ownedForUpdate(Long userId, Long candidateId) {
        if (!users.existsById(userId)) throw new BusinessException(ErrorCode.INVALID_TOKEN);
        Candidate candidate = candidates.findByIdForUpdate(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        CandidateQueryService.validateAccess(userId, candidate);
        return candidate;
    }
}
