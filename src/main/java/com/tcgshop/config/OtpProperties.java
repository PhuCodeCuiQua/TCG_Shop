package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(int length, int validMinutes, int maxAttempts, int resendCooldownSeconds) {
}
