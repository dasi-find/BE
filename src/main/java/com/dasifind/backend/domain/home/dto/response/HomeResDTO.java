package com.dasifind.backend.domain.home.dto.response;

import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HomeResDTO(List<ActiveSearchCardResDTO> activeSearchCards,
                         List<NewCandidateResDTO> newCandidates, long unreadNotificationCount) {
    public HomeResDTO {
        activeSearchCards = List.copyOf(activeSearchCards);
        newCandidates = List.copyOf(newCandidates);
    }

    public record ActiveSearchCardResDTO(Long id, String itemName, SearchCardStatus status,
                                        long daysRemaining, LocalDate lostDate, String lostPlaceName,
                                        BigDecimal bestCandidateScore) {}

    public record NewCandidateResDTO(Long id, Long searchCardId, String itemName,
                                     String storagePlace, BigDecimal totalScore, boolean isNew) {}
}
