package com.dasifind.backend.domain.candidate.dto.request;

import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import jakarta.validation.constraints.NotNull;

public record CandidateFeedbackReqDTO(@NotNull CandidateFeedback feedback) {}
