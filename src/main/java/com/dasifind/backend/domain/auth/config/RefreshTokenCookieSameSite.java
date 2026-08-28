package com.dasifind.backend.domain.auth.config;

public enum RefreshTokenCookieSameSite {
    LAX("Lax"),
    STRICT("Strict"),
    NONE("None");

    private final String attributeValue;

    RefreshTokenCookieSameSite(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String attributeValue() {
        return attributeValue;
    }
}
