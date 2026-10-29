package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPageControllerMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(roles = "USER")
	void notificationsPageRendersSharedBellAndPaginatedList() throws Exception {
		mockMvc.perform(get("/notifications")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("user/notifications"))
				.andExpect(content().string(containsString("notificationBell")))
				.andExpect(content().string(containsString("Tất cả thông báo")))
				.andExpect(content().string(containsString("allNotificationsPagination")));
	}
}
