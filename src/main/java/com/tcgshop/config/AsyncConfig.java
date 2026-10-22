package com.tcgshop.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean(name = "mailExecutor")
	public ThreadPoolTaskExecutor mailExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("mail-");
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return mailExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (Throwable exception, Method method, Object... parameters) -> log.error(
				"Lỗi không xử lý được trong tác vụ async {}", method.getName(), exception);
	}
}
