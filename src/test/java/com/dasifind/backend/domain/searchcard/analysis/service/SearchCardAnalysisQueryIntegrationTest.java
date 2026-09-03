package com.dasifind.backend.domain.searchcard.analysis.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SearchCardAnalysisQueryIntegrationTest {

    @Autowired
    private SearchCardAnalysisQueryService queryService;

    @Autowired
    private SearchCardAnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 읽기_트랜잭션_안에서_지연_로딩_목록을_응답으로_변환한다() {
        User user = userRepository.save(User.create(
                "analysis-query@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardAnalysis analysis = analysisRepository.saveAndFlush(SearchCardAnalysis.create(
                user.getId(),
                new AiAnalysisClientResponse(
                        "WALLET",
                        "CARD_WALLET",
                        List.of("NAVY", "BLACK"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("앞면 은색 로고", "오른쪽 아래 긁힘"),
                        "preprocess-v1"
                )
        ));

        SearchCardAnalysisResponse response = queryService.get(user.getId(), analysis.getId());

        assertThat(response.colors()).containsExactly("NAVY", "BLACK");
        assertThat(response.materials()).containsExactly("LEATHER");
        assertThat(response.features()).containsExactly("앞면 은색 로고", "오른쪽 아래 긁힘");
    }
}
