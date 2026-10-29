package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DemoController {

	private static final int PAGE_SIZE = 3;
	private static final int TOTAL_ITEMS = 12;

	@GetMapping("/demo")
	public String showLayoutDemo(@RequestParam(defaultValue = "0") int page, Model model) {
		int totalPages = (TOTAL_ITEMS + PAGE_SIZE - 1) / PAGE_SIZE;
		int currentPage = Math.max(0, Math.min(page, totalPages - 1));
		List<String> content = IntStream.range(0, PAGE_SIZE)
				.mapToObj(index -> "Mục mẫu " + (currentPage * PAGE_SIZE + index + 1))
				.toList();
		Page<String> demoPage = new PageImpl<>(content, PageRequest.of(currentPage, PAGE_SIZE), TOTAL_ITEMS);

		model.addAttribute("page", demoPage);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Trang chủ", "url", "/"),
				Map.of("label", "Layout demo", "url", "/demo")));
		return "sample/layout-demo";
	}
}
