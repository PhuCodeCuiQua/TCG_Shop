package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.login")
public record LoginProperties(int maxFailedAttempts, long lockMinutes) {
}
