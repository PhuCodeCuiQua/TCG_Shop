package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StaticPageController.class, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class StaticPageControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserDetailsServiceImpl userDetailsService;

	@MockBean
	private RefreshTokenService refreshTokenService;

	@Test
	void staticPagesArePublicAndRenderThroughTheSharedLayout() throws Exception {
		assertPublicPage("/about", "static/about");
		assertPublicPage("/contact", "static/contact");
		assertPublicPage("/faq", "static/faq");
		assertPublicPage("/terms", "static/terms");
		assertPublicPage("/privacy", "static/privacy");
		assertPublicPage("/guide-buy", "static/guide-buy");
		assertPublicPage("/shipping", "static/shipping");
		assertPublicPage("/returns", "static/returns");
	}

	private void assertPublicPage(String url, String viewName) throws Exception {
		mockMvc.perform(get(url))
				.andExpect(status().isOk())
				.andExpect(view().name(viewName))
				.andExpect(content().string(containsString("TCG")))
				.andExpect(content().string(containsString("Help &amp; Contact")));
	}
}
