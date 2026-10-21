package com.tcgshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))


			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/ws/**").permitAll()
				.anyRequest().permitAll());

		return http.build();
	}
}