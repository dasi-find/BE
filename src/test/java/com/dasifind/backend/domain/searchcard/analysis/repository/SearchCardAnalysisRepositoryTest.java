package com.dasifind.backend.domain.searchcard.analysis.repository;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardAnalysisRepositoryTest {

    @Autowired
    private SearchCardAnalysisRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void AI_분석_결과와_순서가_있는_특징_목록을_저장한다() {
        User user = userRepository.save(User.create(
                "analysis-user@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardAnalysis saved = repository.saveAndFlush(SearchCardAnalysis.create(
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
        entityManager.clear();

        SearchCardAnalysis found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getColors()).containsExactly("NAVY", "BLACK");
        assertThat(found.getMaterials()).containsExactly("LEATHER");
        assertThat(found.getFeatures()).containsExactly("앞면 은색 로고", "오른쪽 아래 긁힘");
        assertThat(found.getModelVersion()).isEqualTo("preprocess-v1");
    }
}
