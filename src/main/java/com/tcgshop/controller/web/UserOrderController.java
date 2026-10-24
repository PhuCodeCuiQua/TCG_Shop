package com.tcgshop.controller.web;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.entity.enums.CancelReason;
import com.tcgshop.service.OrderCancellationService;
import com.tcgshop.service.UserOrderService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserOrderController {
	private final UserOrderService userOrderService;
	private final OrderCancellationService orderCancellationService;

	public UserOrderController(UserOrderService userOrderService, OrderCancellationService orderCancellationService) {
		this.userOrderService = userOrderService;
		this.orderCancellationService = orderCancellationService;
	}

	@GetMapping("/orders")
	public String list(@RequestParam(defaultValue = "all") String tab,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		model.addAttribute("orderList", userOrderService.listMyOrders(tab, q, page));
		return "user/orders/index";
	}

	@GetMapping("/orders/{id}")
	public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		model.addAttribute("order", userOrderService.getMyOrder(id));
		model.addAttribute("cancelReasons", CancelReason.values());
		model.addAttribute("cancelForm", new CancelOrderRequest());
		return "user/orders/detail";
	}

	@PostMapping("/orders/{id}/cancel")
	public String cancel(@PathVariable Long id, @Valid @ModelAttribute("cancelForm") CancelOrderRequest cancelForm,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			var firstError = bindingResult.getAllErrors().get(0);
			boolean hasTypeMismatch = bindingResult.getFieldErrors().stream()
					.anyMatch(fieldError -> fieldError.getCodes() != null
							&& java.util.Arrays.stream(fieldError.getCodes()).anyMatch(code -> code.startsWith("typeMismatch")));
			String message = hasTypeMismatch
					? "Vui lòng chọn lý do hủy đơn"
					: firstError.getDefaultMessage();
			redirectAttributes.addFlashAttribute("errorMessage", message);
			return "redirect:/orders/" + id;
		}

		var result = orderCancellationService.cancelMyOrder(id, cancelForm);
		String message = result.alreadyCancelled()
				? "Đơn hàng " + result.orderCode() + " đã được hủy trước đó"
				: "Đã hủy đơn hàng " + result.orderCode();
		redirectAttributes.addFlashAttribute("successMessage", message);
		return "redirect:/orders/" + id;
	}
}
