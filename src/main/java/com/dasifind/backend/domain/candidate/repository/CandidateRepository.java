package com.dasifind.backend.domain.candidate.repository;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findBySearchCardIdAndPoliceItemId(Long searchCardId, Long policeItemId);
}
