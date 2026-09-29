package com.dasifind.backend.domain.candidate.dto.response;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.entity.CandidateEvidence;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CandidateListResDTO(
        List<CandidateListItemResDTO> content, int page, int size, long totalElements, boolean hasNext
) {
    public CandidateListResDTO {
        content = List.copyOf(content);
    }

    public record CandidateListItemResDTO(Long candidateId, Long rank, String itemName, String color,
                       LocalDate foundDate, String storagePlace, String imageUrl,
                       BigDecimal totalScore, BigDecimal evidenceCoverage, String scorePolicyVersion,
                       boolean isNew, CandidateFeedback feedback, List<String> reasons) {
        public static CandidateListItemResDTO from(Candidate candidate, Long rank) {
            var item = candidate.getPoliceItem();
            return new CandidateListItemResDTO(candidate.getId(), rank, item.getItemName(), item.getColor(),
                    item.getFoundDate(), item.getStoragePlace(), item.getImageUrl(),
                    candidate.getTotalScore(), candidate.getEvidenceCoverage(), candidate.getScorePolicyVersion(),
                    candidate.getViewedAt() == null, candidate.getFeedback(),
                    candidate.getEvidence().stream().map(CandidateEvidence::getMessage).toList());
        }
    }
}
