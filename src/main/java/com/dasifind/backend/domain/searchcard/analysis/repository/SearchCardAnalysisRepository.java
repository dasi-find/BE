package com.dasifind.backend.domain.searchcard.analysis.repository;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCardAnalysisRepository extends JpaRepository<SearchCardAnalysis, Long> {
}
