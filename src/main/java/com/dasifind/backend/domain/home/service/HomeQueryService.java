package com.dasifind.backend.domain.home.service;

import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.candidate.service.CandidateSummaryQueryService;
import com.dasifind.backend.domain.candidate.service.CandidateSummaryQueryService.Summary;
import com.dasifind.backend.domain.home.dto.response.HomeResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HomeQueryService {
    private final SearchCardRepository cards;
    private final LostLocationRepository locations;
    private final CandidateRepository candidates;
    private final CandidateSummaryQueryService summaries;
    private final UserRepository users;
    private final Clock homeClock;

    public HomeQueryService(SearchCardRepository cards, LostLocationRepository locations,
                            CandidateRepository candidates, CandidateSummaryQueryService summaries,
                            UserRepository users, Clock homeClock) {
        this.cards = cards;
        this.locations = locations;
        this.candidates = candidates;
        this.summaries = summaries;
        this.users = users;
        this.homeClock = homeClock;
    }

    public HomeResDTO getHome(Long userId) {
        if (!users.existsById(userId)) throw new BusinessException(ErrorCode.INVALID_TOKEN);
        LocalDateTime now = LocalDateTime.now(homeClock);
        var activeCards = cards.findActiveForHome(userId, now);
        if (activeCards.isEmpty()) return new HomeResDTO(List.of(), List.of(), 0);
        var cardIds = activeCards.stream().map(card -> card.getId()).toList();
        Map<Long, LostLocation> places = locations.findAllBySearchCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(LostLocation::getSearchCardId, Function.identity()));
        var stats = summaries.summarize(userId, cardIds);
        var cardDtos = activeCards.stream().map(card -> {
            LostLocation place = places.get(card.getId());
            return new HomeResDTO.ActiveSearchCardResDTO(card.getId(), card.getItemName(), card.getStatus(),
                    ChronoUnit.DAYS.between(now.toLocalDate(), card.getSearchExpiresAt().toLocalDate()),
                    card.getLostDate(), place == null ? null : place.getPlaceName(),
                    stats.getOrDefault(card.getId(), Summary.EMPTY).bestCandidateScore());
        }).toList();
        var newCandidates = candidates.findNewForHome(userId, now, PageRequest.of(0, 5)).stream()
                .map(candidate -> new HomeResDTO.NewCandidateResDTO(candidate.getId(),
                        candidate.getSearchCard().getId(), candidate.getPoliceItem().getItemName(),
                        candidate.getPoliceItem().getStoragePlace(), candidate.getTotalScore(), true))
                .toList();
        // Notification persistence is not implemented yet; replace with its unread count when integrated.
        return new HomeResDTO(cardDtos, newCandidates, 0);
    }
}
