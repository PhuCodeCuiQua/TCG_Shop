package com.tcgshop.config;

import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtAuthenticationFilter;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/** Central URL authorization, JWT stateless authentication and CSRF policy. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtProvider jwtProvider;
	private final CookieUtil cookieUtil;
	private final UserDetailsServiceImpl userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;
	private final RefreshTokenService refreshTokenService;

	public SecurityConfig(JwtProvider jwtProvider, CookieUtil cookieUtil,
			UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder,
			JsonAuthenticationEntryPoint authenticationEntryPoint,
			JsonAccessDeniedHandler accessDeniedHandler, RefreshTokenService refreshTokenService) {
		this.jwtProvider = jwtProvider;
		this.cookieUtil = cookieUtil;
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
		this.refreshTokenService = refreshTokenService;
	}

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			DaoAuthenticationProvider provider) throws Exception {
		JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
				jwtProvider, cookieUtil, userDetailsService, refreshTokenService);
		http
			.authenticationProvider(provider)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.ignoringRequestMatchers("/payment/momo/ipn", "/ws/**"))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.logout(logout -> logout.disable())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/auth/**", "/error", "/favicon.ico", "/ws/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/payment/momo/ipn").permitAll()
				.requestMatchers(HttpMethod.GET, "/payment/momo/return").permitAll()
				.requestMatchers("/css/**", "/js/**", "/img/**", "/lib/**").permitAll()
				.requestMatchers(HttpMethod.GET,
					"/", "/products/**", "/cards/**", "/shops/**", "/games/**", "/series/**", "/search",
					"/api/products/**", "/api/cards/**", "/api/shops/**", "/api/games/**", "/api/series/**",
					"/api/public-test")
					.permitAll()
				.requestMatchers("/profile/**", "/notifications/**", "/api/profile/**", "/api/notifications/**")
				.authenticated()
				.requestMatchers("/cart/**", "/checkout/**", "/orders/**", "/wishlist/**", "/viewed/**",
					"/check-my-card/**", "/seller-registration/**", "/api/cart/**", "/api/wishlist/**",
					"/api/reviews/**", "/api/price-alerts/**").hasRole("USER")
				.requestMatchers("/vendor/**", "/api/vendor/**").hasRole("VENDOR")
				.requestMatchers("/shipper/**", "/api/shipper/**").hasRole("SHIPPER")
				.requestMatchers("/manager/**", "/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
