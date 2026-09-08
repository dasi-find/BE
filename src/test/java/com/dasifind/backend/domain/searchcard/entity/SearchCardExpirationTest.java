package com.dasifind.backend.domain.searchcard.entity;

import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCardExpirationTest {

    @Test
    void 생성_30일째_마지막_시각까지_유효하고_그_이후에는_비활성이다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        SearchCard card = SearchCard.create(1L, 1L, "WALLET", "지갑", List.of(),
                null, null, "특징", createdAt.toLocalDate(), null, null, createdAt);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        assertThat(card.getSearchExpiresAt()).isEqualTo(expiresAt);
        assertThat(card.isActiveAt(expiresAt.minusSeconds(1))).isTrue();
        assertThat(card.isActiveAt(expiresAt)).isTrue();
        assertThat(card.isActiveAt(expiresAt.plusNanos(1))).isFalse();
        for (SearchCardStatus status : List.of(SearchCardStatus.FOUND, SearchCardStatus.CLOSED,
                SearchCardStatus.EXPIRED)) {
            ReflectionTestUtils.setField(card, "status", status);
            assertThat(card.isActiveAt(createdAt)).isFalse();
        }
    }
}
