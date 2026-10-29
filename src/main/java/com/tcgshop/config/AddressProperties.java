package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.address")
public record AddressProperties(int maxPerUser) {
}
