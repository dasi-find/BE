package com.dasifind.backend.domain.user.service;

import com.dasifind.backend.domain.user.dto.request.UpdateMyProfileRequest;
import com.dasifind.backend.domain.user.dto.response.UpdateMyProfileResponse;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandService {

    private final UserRepository userRepository;

    public UserCommandService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UpdateMyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        String normalizedName = normalizeName(request.name());
        if (normalizedName == null && request.emailNotificationEnabled() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        user.updateProfile(normalizedName, request.emailNotificationEnabled());
        return UpdateMyProfileResponse.from(user);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalizedName = name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return normalizedName;
    }
}
