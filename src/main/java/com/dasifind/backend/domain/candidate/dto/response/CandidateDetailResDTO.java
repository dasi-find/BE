package com.dasifind.backend.domain.candidate.dto.response;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.entity.CandidateScores;
import com.dasifind.backend.domain.candidate.entity.CandidateEvidence;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import com.dasifind.backend.domain.candidate.model.EvidenceType;
import com.dasifind.backend.domain.candidate.model.ScoreElement;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CandidateDetailResDTO(
        Long candidateId, Long searchCardId, BigDecimal totalScore, Long rank,
        CandidateFeedback feedback, boolean isExcluded, BigDecimal evidenceCoverage,
        String scorePolicyVersion, PoliceItemResDTO policeItem, ScoresResDTO scores,
        List<EvidenceResDTO> evidenceDetails, List<String> reasons
) {
    public static CandidateDetailResDTO from(Candidate candidate, Long rank) {
        var evidence = candidate.getEvidence();
        return new CandidateDetailResDTO(candidate.getId(), candidate.getSearchCard().getId(),
                candidate.getTotalScore(), rank, candidate.getFeedback(), candidate.isExcluded(),
                candidate.getEvidenceCoverage(), candidate.getScorePolicyVersion(),
                PoliceItemResDTO.from(candidate.getPoliceItem()), ScoresResDTO.from(candidate.getScores()),
                evidence.stream().map(e -> new EvidenceResDTO(e.getType(), e.getElement(), e.getMessage())).toList(),
                evidence.stream().map(CandidateEvidence::getMessage).toList());
    }

    public record PoliceItemResDTO(PoliceItemSource source, int itemSequence, String foundPlace,
                                   String description, String itemName, String category, String color,
                                   LocalDate foundDate, String storagePlace, String policeManagementNo,
                                   String imageUrl, String originalUrl) {
        static PoliceItemResDTO from(PoliceItem item) {
            return new PoliceItemResDTO(item.getSource(), item.getItemSequence(), item.getFoundPlace(),
                    item.getDescription(), item.getItemName(), item.getCategory(), item.getColor(),
                    item.getFoundDate(), item.getStoragePlace(), item.getManagementNo(),
                    item.getImageUrl(), item.getOriginalUrl());
        }
    }

    public record ScoresResDTO(BigDecimal imageScore, BigDecimal textScore, BigDecimal imageTextScore,
                               BigDecimal attributeScore, BigDecimal dateScore, BigDecimal locationScore) {
        static ScoresResDTO from(CandidateScores scores) {
            return new ScoresResDTO(scores.getImageScore(), scores.getTextScore(), scores.getImageTextScore(),
                    scores.getAttributeScore(), scores.getDateScore(), scores.getLocationScore());
        }
    }

    public record EvidenceResDTO(EvidenceType type, ScoreElement element, String message) {}
}
