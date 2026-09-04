package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCardRepository extends JpaRepository<SearchCard, Long> {

    boolean existsByAnalysisId(Long analysisId);
}
