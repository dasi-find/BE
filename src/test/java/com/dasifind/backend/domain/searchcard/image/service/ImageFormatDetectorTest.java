package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.model.DetectedImageFormat;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFormatDetectorTest {

    private final ImageFormatDetector detector = new ImageFormatDetector();

    @Test
    void JPEG_시그니처를_감지한다() {
        DetectedImageFormat format = detector.detect(new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00
        });

        assertThat(format).isEqualTo(new DetectedImageFormat("image/jpeg", "jpg"));
    }

    @Test
    void PNG_시그니처를_감지한다() {
        DetectedImageFormat format = detector.detect(new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        });

        assertThat(format).isEqualTo(new DetectedImageFormat("image/png", "png"));
    }

    @Test
    void WebP_시그니처를_감지한다() {
        DetectedImageFormat format = detector.detect(new byte[]{
                0x52, 0x49, 0x46, 0x46, 0x04, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        });

        assertThat(format).isEqualTo(new DetectedImageFormat("image/webp", "webp"));
    }

    @Test
    void 확장자와_MIME_선언과_무관하게_지원하지_않는_바이트는_거절한다() {
        assertThatThrownBy(() -> detector.detect("not-an-image".getBytes()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }
}
