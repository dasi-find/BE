package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.dto.request.EmailVerificationConfirmRequest;
import com.dasifind.backend.domain.auth.dto.request.EmailVerificationSendRequest;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationSendResponse;
import com.dasifind.backend.domain.auth.service.EmailVerificationService;
import com.dasifind.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping
    public ApiResponse<EmailVerificationSendResponse> send(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        return ApiResponse.success(emailVerificationService.send(request.email()));
    }

    @PostMapping("/{verificationId}/confirm")
    public ApiResponse<EmailVerificationConfirmResponse> confirm(
            @PathVariable String verificationId,
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ApiResponse.success(emailVerificationService.confirm(
                verificationId,
                request.verificationCode()
        ));
    }
}
