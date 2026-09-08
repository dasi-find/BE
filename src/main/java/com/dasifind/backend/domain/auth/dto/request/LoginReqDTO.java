package com.dasifind.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginReqDTO(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password
) {

    public LoginReqDTO {
        email = email == null ? null : email.trim();
    }
}
