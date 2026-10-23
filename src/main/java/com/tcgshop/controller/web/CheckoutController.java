package com.tcgshop.controller.web;

import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.service.CartService;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {
	private final CartService cartService;

	public CheckoutController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping("/checkout")
	public String checkout(Model model, RedirectAttributes redirectAttributes) {
		CartResponse cart = cartService.getCart();
		if (cart == null || cart.selectedItemCount() == 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm để thanh toán");
			return "redirect:/cart";
		}

		// Token được sinh ở server cho mỗi lần mở trang để request đặt hàng sau này có khóa idempotency riêng.
		model.addAttribute("idempotencyToken", UUID.randomUUID().toString());
		model.addAttribute("title", "Thanh toán");
		return "user/checkout";
	}
}
