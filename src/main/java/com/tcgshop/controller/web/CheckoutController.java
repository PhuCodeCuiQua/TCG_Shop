package com.tcgshop.controller.web;

import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.service.CartService;
import com.tcgshop.service.OrderPlacementService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {
	private final CartService cartService;
	private final OrderPlacementService orderPlacementService;

	public CheckoutController(CartService cartService, OrderPlacementService orderPlacementService) {
		this.cartService = cartService;
		this.orderPlacementService = orderPlacementService;
	}

	@GetMapping("/checkout")
	public String checkout(Model model, RedirectAttributes redirectAttributes, HttpServletResponse response,
			@RequestParam(defaultValue = "false") boolean paymentCancelled) {
		// Trang chứa token chống đặt trùng; không lưu cache để Back không khôi phục token đã dùng.
		response.setHeader("Cache-Control", "no-store");
		CartResponse cart = cartService.getCart();
		if (cart == null || cart.selectedItemCount() == 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm để thanh toán");
			return "redirect:/cart";
		}

		// Token được sinh ở server cho mỗi lần mở trang để request đặt hàng sau này có khóa idempotency riêng.
		model.addAttribute("idempotencyToken", UUID.randomUUID().toString());
		model.addAttribute("title", "Thanh toán");
		if (paymentCancelled) {
			model.addAttribute("errorMessage", "Thanh toán đã bị hủy. Bạn có thể thử lại.");
		}
		return "user/checkout";
	}

	@GetMapping("/checkout/success/{groupCode}")
	public String success(@PathVariable String groupCode,
			Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		model.addAttribute("summary", orderPlacementService.getPlacedSummary(groupCode));
		return "user/checkout-success";
	}
}
