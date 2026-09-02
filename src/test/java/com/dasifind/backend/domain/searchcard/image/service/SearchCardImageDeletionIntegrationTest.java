package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class SearchCardImageDeletionIntegrationTest {

    @Autowired
    private SearchCardImageService searchCardImageService;

    @Autowired
    private SearchCardImageRepository searchCardImageRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ImageStorage imageStorage;

    @Test
    void S3_삭제가_실패하면_DB_삭제를_롤백한다() {
        User user = userRepository.save(User.create(
                "rollback-image@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardImage image = searchCardImageRepository.saveAndFlush(SearchCardImage.create(
                user.getId(),
                "search-card-images/" + user.getId() + "/rollback.png",
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        ));
        doThrow(new RuntimeException("S3 unavailable"))
                .when(imageStorage).delete(image.getStorageKey());

        assertThatThrownBy(() -> searchCardImageService.delete(user.getId(), image.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 unavailable");

        assertThat(searchCardImageRepository.existsById(image.getId())).isTrue();
    }
}
