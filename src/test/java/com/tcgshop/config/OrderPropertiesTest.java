package com.tcgshop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class OrderPropertiesTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void defaultsReturnWindowToSevenDaysWhenNotConfigured() {
		contextRunner.run(context -> {
			assertEquals(7, context.getBean(OrderProperties.class).returnWindowDays());
			assertEquals(3, context.getBean(OrderProperties.class).escalationWindowDays());
		});
	}

	@Test
	void bindsConfiguredReturnWindow() {
		contextRunner.withPropertyValues("app.order.return-window-days=14").run(context -> {
			assertEquals(14, context.getBean(OrderProperties.class).returnWindowDays());
			assertEquals(3, context.getBean(OrderProperties.class).escalationWindowDays());
		});
	}

	@Test
	void bindsConfiguredEscalationWindow() {
		contextRunner.withPropertyValues("app.order.escalation-window-days=5").run(context -> {
			assertEquals(5, context.getBean(OrderProperties.class).escalationWindowDays());
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OrderProperties.class)
	static class TestConfiguration {
	}
}
