package com.dasifind.backend.domain.user.controller;

import com.dasifind.backend.domain.user.dto.request.UpdateMyProfileRequest;
import com.dasifind.backend.domain.user.dto.response.MyProfileResponse;
import com.dasifind.backend.domain.user.dto.response.UpdateMyProfileResponse;
import com.dasifind.backend.domain.user.service.UserCommandService;
import com.dasifind.backend.domain.user.service.UserQueryService;
import com.dasifind.backend.global.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    public UserController(
            UserQueryService userQueryService,
            UserCommandService userCommandService
    ) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        MyProfileResponse response = userQueryService.getMyProfile(Long.valueOf(jwt.getSubject()));
        return ApiResponse.success(response);
    }

    @PatchMapping("/me")
    public ApiResponse<UpdateMyProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateMyProfileRequest request
    ) {
        UpdateMyProfileResponse response = userCommandService.updateMyProfile(
                Long.valueOf(jwt.getSubject()),
                request
        );
        return ApiResponse.success(response);
    }
}
