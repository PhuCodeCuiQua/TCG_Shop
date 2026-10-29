package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(@DefaultValue("7") int returnWindowDays,
		@DefaultValue("3") int escalationWindowDays) {
}
