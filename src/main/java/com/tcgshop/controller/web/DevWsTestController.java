package com.tcgshop.controller.web;

import com.tcgshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** TẠM THỜI cho PA-05: trang thử WebSocket. XÓA trước khi merge. Chỉ chạy ở profile local. */
@Profile("local")
@Controller
@RequiredArgsConstructor
public class DevWsTestController {

	private final CurrentUser currentUser;

	@GetMapping("/dev/ws-test")
	public String page(Model model) {
		boolean authenticated = currentUser.isAuthenticated();
		model.addAttribute("authenticated", authenticated);
		model.addAttribute("email", authenticated ? currentUser.getEmail() : null);
		return "dev/ws-test";
	}
}