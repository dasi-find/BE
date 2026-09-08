package com.dasifind.backend.domain.searchcard.image.repository;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImageDeletionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchCardImageDeletionTaskRepository
        extends JpaRepository<SearchCardImageDeletionTask, Long> {

    List<SearchCardImageDeletionTask> findTop100ByOrderByIdAsc();
}
