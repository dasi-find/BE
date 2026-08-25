package com.dasifind.backend.domain.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties(EmailVerificationProperties.class)
public class AuthConfig {

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
