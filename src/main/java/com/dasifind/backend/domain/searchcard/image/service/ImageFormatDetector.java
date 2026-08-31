package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.model.DetectedImageFormat;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ImageFormatDetector {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public DetectedImageFormat detect(byte[] content) {
        if (isJpeg(content)) {
            return new DetectedImageFormat("image/jpeg", "jpg");
        }
        if (startsWith(content, PNG_SIGNATURE)) {
            return new DetectedImageFormat("image/png", "png");
        }
        if (isWebp(content)) {
            return new DetectedImageFormat("image/webp", "webp");
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF;
    }

    private boolean isWebp(byte[] content) {
        return content.length >= 12
                && matches(content, 0, new byte[]{0x52, 0x49, 0x46, 0x46})
                && matches(content, 8, new byte[]{0x57, 0x45, 0x42, 0x50});
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        return matches(content, 0, signature);
    }

    private boolean matches(byte[] content, int offset, byte[] signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[offset + index] != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
