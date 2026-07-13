package com.bharath.financetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Injected wherever "now" matters (token expiry, upcoming SIP/maturity windows) so tests can
 * pin the current date instead of depending on the wall clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
