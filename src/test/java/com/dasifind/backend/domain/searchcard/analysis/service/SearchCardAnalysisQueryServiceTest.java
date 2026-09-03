package com.dasifind.backend.domain.searchcard.analysis.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardAnalysisQueryServiceTest {

    @Mock
    private SearchCardAnalysisRepository repository;

    @Mock
    private UserRepository userRepository;

    private SearchCardAnalysisQueryService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardAnalysisQueryService(repository, userRepository);
    }

    @Test
    void 본인의_AI_분석_결과를_조회한다() {
        SearchCardAnalysis analysis = analysis(7L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(repository.findById(801L)).thenReturn(Optional.of(analysis));

        SearchCardAnalysisResponse response = service.get(7L, 801L);

        assertThat(response.analysisId()).isEqualTo(801L);
        assertThat(response.colors()).containsExactly("NAVY", "BLACK");
        assertThat(response.materials()).containsExactly("LEATHER");
        assertThat(response.features()).containsExactly("앞면 은색 로고");
    }

    @Test
    void 존재하지_않는_분석_결과는_조회할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(repository.findById(801L)).thenReturn(Optional.empty());

        assertError(7L, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void 타인의_분석_결과는_조회할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(repository.findById(801L)).thenReturn(Optional.of(analysis(8L)));

        assertError(7L, ErrorCode.FORBIDDEN);
    }

    @Test
    void 토큰의_사용자가_없으면_분석_결과를_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(7L, ErrorCode.INVALID_TOKEN);

        verify(repository, never()).findById(any());
    }

    private void assertError(Long userId, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> service.get(userId, 801L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private SearchCardAnalysis analysis(Long userId) {
        SearchCardAnalysis analysis = SearchCardAnalysis.create(
                userId,
                new AiAnalysisClientResponse(
                        "WALLET",
                        "CARD_WALLET",
                        List.of("NAVY", "BLACK"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("앞면 은색 로고"),
                        "preprocess-v1"
                )
        );
        ReflectionTestUtils.setField(analysis, "id", 801L);
        return analysis;
    }
}
