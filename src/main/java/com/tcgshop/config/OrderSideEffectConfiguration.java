package com.tcgshop.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrderSideEffectConfiguration {
	/** Dùng múi giờ hệ thống cho mốc giao/hoàn tiền; unit test có thể truyền Clock cố định. */
	@Bean
	Clock orderClock() {
		return Clock.systemDefaultZone();
	}
}
