package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.config.SearchCardImageProperties;
import com.dasifind.backend.domain.searchcard.image.dto.response.SearchCardImageUploadResponse;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardImageServiceTest {

    @Mock
    private SearchCardImageRepository searchCardImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageStorage imageStorage;

    private SearchCardImageService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardImageService(
                searchCardImageRepository,
                userRepository,
                imageStorage,
                new ImageFormatDetector(),
                new SearchCardImageProperties(DataSize.ofMegabytes(10), Duration.ofHours(24))
        );
    }

    @Test
    void 실제_이미지_형식을_감지해_S3와_DB에_저장한다() {
        byte[] png = pngContent();
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "text/plain", png);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(imageStorage.createDownloadUrl(anyString())).thenReturn("https://presigned.example/image");
        when(searchCardImageRepository.saveAndFlush(any(SearchCardImage.class)))
                .thenAnswer(invocation -> {
                    SearchCardImage image = invocation.getArgument(0);
                    ReflectionTestUtils.setField(image, "id", 501L);
                    return image;
                });

        SearchCardImageUploadResponse response = service.upload(
                7L,
                file,
                SearchCardImageType.REFERENCE
        );

        ArgumentCaptor<SearchCardImage> imageCaptor = ArgumentCaptor.forClass(SearchCardImage.class);
        verify(searchCardImageRepository).saveAndFlush(imageCaptor.capture());
        SearchCardImage savedImage = imageCaptor.getValue();
        assertThat(savedImage.getStorageKey())
                .startsWith("search-card-images/7/")
                .endsWith(".png");
        assertThat(savedImage.getContentType()).isEqualTo("image/png");
        assertThat(savedImage.getFileSize()).isEqualTo(png.length);
        verify(imageStorage).upload(savedImage.getStorageKey(), "image/png", png);
        assertThat(response.imageId()).isEqualTo(501L);
        assertThat(response.imageUrl()).isEqualTo("https://presigned.example/image");
        assertThat(response.imageType()).isEqualTo(SearchCardImageType.REFERENCE);
    }

    @Test
    void 파일이_10MB를_초과하면_S3를_호출하지_않는다() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(file.getSize()).thenReturn(DataSize.ofMegabytes(10).toBytes() + 1);

        assertError(file, ErrorCode.IMAGE_TOO_LARGE);

        verify(imageStorage, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void 지원하지_않는_파일_바이트는_거절한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "not-an-image".getBytes()
        );
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(file, ErrorCode.UNSUPPORTED_IMAGE_TYPE);

        verify(imageStorage, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void DB_저장에_실패하면_업로드한_S3_객체를_삭제한다() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngContent());
        when(userRepository.existsById(7L)).thenReturn(true);
        when(imageStorage.createDownloadUrl(anyString())).thenReturn("https://presigned.example/image");
        RuntimeException saveFailure = new RuntimeException("save failed");
        when(searchCardImageRepository.saveAndFlush(any(SearchCardImage.class))).thenThrow(saveFailure);

        assertThatThrownBy(() -> service.upload(7L, file, SearchCardImageType.ACTUAL))
                .isSameAs(saveFailure);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageStorage).upload(keyCaptor.capture(), anyString(), any());
        verify(imageStorage).delete(keyCaptor.getValue());
    }

    @Test
    void 토큰의_사용자가_없으면_업로드하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(
                7L,
                new MockMultipartFile("file", pngContent()),
                SearchCardImageType.ACTUAL
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));

        verify(imageStorage, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void 본인이_업로드한_연결된_이미지를_DB와_S3에서_삭제한다() {
        SearchCardImage image = image(7L, "search-card-images/7/image.png");
        ReflectionTestUtils.setField(image, "searchCardId", 31L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardImageRepository.findById(501L)).thenReturn(Optional.of(image));

        service.delete(7L, 501L);

        var inOrder = inOrder(searchCardImageRepository, imageStorage);
        inOrder.verify(searchCardImageRepository).delete(image);
        inOrder.verify(searchCardImageRepository).flush();
        inOrder.verify(imageStorage).delete("search-card-images/7/image.png");
    }

    @Test
    void 존재하지_않는_이미지는_삭제할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardImageRepository.findById(501L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L, 501L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(searchCardImageRepository, never()).delete(any());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void 타인이_업로드한_이미지는_삭제할_수_없다() {
        SearchCardImage image = image(8L, "search-card-images/8/image.png");
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardImageRepository.findById(501L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> service.delete(7L, 501L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(searchCardImageRepository, never()).delete(any());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void S3_삭제가_실패하면_예외를_전파한다() {
        SearchCardImage image = image(7L, "search-card-images/7/image.png");
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardImageRepository.findById(501L)).thenReturn(Optional.of(image));
        RuntimeException storageFailure = new RuntimeException("delete failed");
        doThrow(storageFailure).when(imageStorage).delete(image.getStorageKey());

        assertThatThrownBy(() -> service.delete(7L, 501L)).isSameAs(storageFailure);

        verify(searchCardImageRepository).delete(image);
        verify(searchCardImageRepository).flush();
    }

    @Test
    void 토큰의_사용자가_없으면_이미지를_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(7L, 501L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));

        verify(searchCardImageRepository, never()).findById(any());
        verify(imageStorage, never()).delete(anyString());
    }

    private void assertError(MultipartFile file, ErrorCode errorCode) {
        assertThatThrownBy(() -> service.upload(7L, file, SearchCardImageType.ACTUAL))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private byte[] pngContent() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};
    }

    private SearchCardImage image(Long userId, String storageKey) {
        return SearchCardImage.create(
                userId,
                storageKey,
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        );
    }
}
