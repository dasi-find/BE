package com.dasifind.backend.domain.user.service;

import com.dasifind.backend.domain.user.dto.request.UpdateMyProfileReqDTO;
import com.dasifind.backend.domain.user.dto.response.UpdateMyProfileResDTO;
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
    public UpdateMyProfileResDTO updateMyProfile(Long userId, UpdateMyProfileReqDTO request) {
        String normalizedName = normalizeName(request.name());
        if (normalizedName == null && request.emailNotificationEnabled() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        user.updateProfile(normalizedName, request.emailNotificationEnabled());
        return UpdateMyProfileResDTO.from(user);
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
