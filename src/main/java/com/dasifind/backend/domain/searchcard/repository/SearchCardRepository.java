package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCardRepository extends JpaRepository<SearchCard, Long> {

    boolean existsByAnalysisId(Long analysisId);

    Page<SearchCard> findByUserId(Long userId, Pageable pageable);

    Page<SearchCard> findByUserIdAndStatus(
            Long userId,
            SearchCardStatus status,
            Pageable pageable
    );
}
