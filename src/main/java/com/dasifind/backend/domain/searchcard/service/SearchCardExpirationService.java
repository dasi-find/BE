package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SearchCardExpirationService {

    private static final Logger log = LoggerFactory.getLogger(SearchCardExpirationService.class);

    private final SearchCardRepository repository;

    public SearchCardExpirationService(SearchCardRepository repository) {
        this.repository = repository;
    }

    @Scheduled(
            fixedDelayString = "${app.search-card.expiration-interval:1m}",
            initialDelayString = "${app.search-card.expiration-initial-delay:10s}"
    )
    @Transactional
    public void expireDueCards() {
        int count = repository.expireDueCards(LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} search cards", count);
        }
    }
}
