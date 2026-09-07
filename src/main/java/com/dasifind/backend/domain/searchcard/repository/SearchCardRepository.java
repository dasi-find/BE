package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SearchCardRepository extends JpaRepository<SearchCard, Long> {

    boolean existsByAnalysisId(Long analysisId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select searchCard from SearchCard searchCard where searchCard.id = :id")
    Optional<SearchCard> findByIdForUpdate(@Param("id") Long id);

    Page<SearchCard> findByUserId(Long userId, Pageable pageable);

    Page<SearchCard> findByUserIdAndStatus(
            Long userId,
            SearchCardStatus status,
            Pageable pageable
    );
}
