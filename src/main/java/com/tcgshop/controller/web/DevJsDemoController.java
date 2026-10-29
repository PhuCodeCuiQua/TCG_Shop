package com.tcgshop.controller.web;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Profile("local")
@Controller
public class DevJsDemoController {
	// TODO: Xóa trước khi merge.
	@GetMapping("/dev/js-demo")
	public String showDemo() {
		return "dev/js-demo";
	}

	// TODO: Xóa trước khi merge. Endpoint chỉ trả JSON mẫu để kiểm tra fetchWithCsrf.
	@ResponseBody
	@GetMapping("/dev/js-demo/sample")
	public Map<String, String> sampleJson() {
		return Map.of("status", "ok", "message", "Fetch demo hoạt động.");
	}
}
