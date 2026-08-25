package com.dasifind.backend.domain.auth.mail;

public interface VerificationEmailSender {

    void sendVerificationCode(String email, String verificationCode, long expiresInMinutes);
}
