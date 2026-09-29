package com.dasifind.backend.domain.candidate.model;

import java.math.BigDecimal;

public final class ScoreValues {
    private ScoreValues() {
    }

    public static BigDecimal unit(BigDecimal value) {
        return validate(value, BigDecimal.ONE, 6);
    }

    public static BigDecimal total(BigDecimal value) {
        return validate(value, new BigDecimal("100"), 4);
    }

    private static BigDecimal validate(BigDecimal value, BigDecimal max, int scale) {
        if (value != null && (value.signum() < 0 || value.compareTo(max) > 0
                || value.stripTrailingZeros().scale() > scale)) {
            throw new IllegalArgumentException("Score outside storage range or precision");
        }
        return value;
    }
}
