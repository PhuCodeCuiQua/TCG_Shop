package com.tcgshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

	@GetMapping("/cart")
	public String cart(Model model) {
		model.addAttribute("title", "Giỏ hàng");
		return "user/cart";
	}
}
