package com.dasifind.backend.domain.home.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class HomeClockConfiguration {
    @Bean
    public Clock homeClock() {
        return Clock.systemDefaultZone();
    }
}
