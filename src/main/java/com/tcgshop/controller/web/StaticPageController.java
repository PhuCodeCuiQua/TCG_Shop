package com.tcgshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticPageController {

	@GetMapping("/about")
	public String about() {
		return "static/about";
	}

	@GetMapping("/contact")
	public String contact() {
		return "static/contact";
	}

	@GetMapping("/faq")
	public String faq() {
		return "static/faq";
	}

	@GetMapping("/terms")
	public String terms() {
		return "static/terms";
	}

	@GetMapping("/privacy")
	public String privacy() {
		return "static/privacy";
	}

	@GetMapping("/guide-buy")
	public String guideBuy() {
		return "static/guide-buy";
	}

	@GetMapping("/shipping")
	public String shipping() {
		return "static/shipping";
	}

	@GetMapping("/returns")
	public String returns() {
		return "static/returns";
	}
}
