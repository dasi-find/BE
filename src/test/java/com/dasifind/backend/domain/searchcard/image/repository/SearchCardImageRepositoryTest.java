package com.dasifind.backend.domain.searchcard.image.repository;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardImageRepositoryTest {

    @Autowired
    private SearchCardImageRepository searchCardImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 생성된지_24시간이_지난_미연결_이미지만_조회한다() {
        User user = userRepository.save(User.create(
                "image-user@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardImage expiredImage = image(user.getId(), "expired.png");
        ReflectionTestUtils.setField(expiredImage, "createdAt", LocalDateTime.now().minusHours(25));
        SearchCardImage freshImage = image(user.getId(), "fresh.png");
        searchCardImageRepository.saveAllAndFlush(List.of(expiredImage, freshImage));

        List<SearchCardImage> result = searchCardImageRepository
                .findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(
                        LocalDateTime.now().minusHours(24)
                );

        assertThat(result).extracting(SearchCardImage::getStorageKey)
                .containsExactly("search-card-images/" + user.getId() + "/expired.png");
    }

    private SearchCardImage image(Long userId, String fileName) {
        return SearchCardImage.create(
                userId,
                "search-card-images/" + userId + "/" + fileName,
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        );
    }
}
