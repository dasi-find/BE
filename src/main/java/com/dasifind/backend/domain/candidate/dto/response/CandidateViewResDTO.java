package com.dasifind.backend.domain.candidate.dto.response;

import java.time.LocalDateTime;

public record CandidateViewResDTO(Long candidateId, LocalDateTime viewedAt) {}
