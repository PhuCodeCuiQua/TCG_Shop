package com.tcgshop.controller.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("isAuthenticated()")
public class NotificationPageController {

	@GetMapping("/notifications")
	public String list() {
		return "user/notifications";
	}
}
