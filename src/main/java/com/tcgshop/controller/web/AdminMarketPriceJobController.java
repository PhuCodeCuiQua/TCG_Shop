package com.tcgshop.controller.web;

import com.tcgshop.dto.response.MarketPriceJobSummary;
import com.tcgshop.service.MarketPriceJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMarketPriceJobController {
	private final MarketPriceJobService marketPriceJobService;

	@PostMapping("/admin/market-price/run")
	public String runNow(RedirectAttributes redirectAttributes) {
		MarketPriceJobSummary summary = marketPriceJobService.runNow();
		redirectAttributes.addFlashAttribute("successMessage",
				"Đã cập nhật giá cho " + summary.processedCards() + " thẻ; "
						+ summary.updatedCards() + " giá thay đổi; "
						+ summary.publishedEvents() + " thông báo giá được phát.");
		return "redirect:/manager/price-history";
	}
}
