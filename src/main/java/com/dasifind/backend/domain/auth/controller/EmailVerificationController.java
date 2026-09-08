package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.dto.request.EmailVerificationConfirmReqDTO;
import com.dasifind.backend.domain.auth.dto.request.EmailVerificationSendReqDTO;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationConfirmResDTO;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationSendResDTO;
import com.dasifind.backend.domain.auth.service.EmailVerificationService;
import com.dasifind.backend.global.api.ApiResDTO;
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
    public ApiResDTO<EmailVerificationSendResDTO> send(
            @Valid @RequestBody EmailVerificationSendReqDTO request
    ) {
        return ApiResDTO.success(emailVerificationService.send(request.email()));
    }

    @PostMapping("/{verificationId}/confirm")
    public ApiResDTO<EmailVerificationConfirmResDTO> confirm(
            @PathVariable String verificationId,
            @Valid @RequestBody EmailVerificationConfirmReqDTO request
    ) {
        return ApiResDTO.success(emailVerificationService.confirm(
                verificationId,
                request.verificationCode()
        ));
    }
}
