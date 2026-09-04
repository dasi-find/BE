package com.dasifind.backend.domain.searchcard.analysis.repository;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SearchCardAnalysisRepository extends JpaRepository<SearchCardAnalysis, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select analysis from SearchCardAnalysis analysis where analysis.id = :id")
    Optional<SearchCardAnalysis> findByIdForUpdate(@Param("id") Long id);
}
