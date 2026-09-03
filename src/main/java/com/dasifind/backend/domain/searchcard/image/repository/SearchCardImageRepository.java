package com.dasifind.backend.domain.searchcard.image.repository;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SearchCardImageRepository extends JpaRepository<SearchCardImage, Long> {

    List<SearchCardImage> findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(
            LocalDateTime createdAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SearchCardImage> findAllByIdInOrderByIdAsc(Collection<Long> ids);
}
