package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LostLocationRepository extends JpaRepository<LostLocation, Long> {

    Optional<LostLocation> findBySearchCardId(Long searchCardId);
}
