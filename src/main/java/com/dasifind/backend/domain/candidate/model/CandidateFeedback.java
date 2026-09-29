package com.dasifind.backend.domain.candidate.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CandidateFeedback {
    VERY_SIMILAR, UNSURE, NOT_MINE;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CandidateFeedback fromJson(Object value) {
        if (value == null) return null;
        if (value instanceof String name) return valueOf(name);
        throw new IllegalArgumentException("Feedback must be an explicit enum name");
    }
}
