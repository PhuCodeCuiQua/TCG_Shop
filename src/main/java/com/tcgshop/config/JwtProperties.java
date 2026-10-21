package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		String secret,
		long accessExpirationMinutes,
		long refreshExpirationDays,
		String accessCookieName,
		String refreshCookieName) {
}
