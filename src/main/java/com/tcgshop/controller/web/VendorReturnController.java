package com.tcgshop.controller.web;

import com.tcgshop.service.VendorReturnService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/vendor/returns")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorReturnController {
	private static final int PAGE_SIZE = 10;
	private final VendorReturnService vendorReturnService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("returnsPage", vendorReturnService.getPendingReturns(
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.unsorted())));
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Yêu cầu trả hàng");
		model.addAttribute("currentPath", "/vendor/returns");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Yêu cầu trả hàng", "url", "/vendor/returns")));
		return "vendor/returns";
	}
}
