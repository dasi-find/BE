package com.dasifind.backend.domain.searchcard.image.repository;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchCardImageRepository extends JpaRepository<SearchCardImage, Long> {

    List<SearchCardImage> findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(
            LocalDateTime createdAt
    );
}
