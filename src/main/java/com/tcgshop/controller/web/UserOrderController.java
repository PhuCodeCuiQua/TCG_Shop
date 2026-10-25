package com.tcgshop.controller.web;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.dto.request.CreateReturnRequest;
import com.tcgshop.dto.request.EscalateReturnRequest;
import com.tcgshop.entity.enums.ReturnReason;
import com.tcgshop.entity.enums.CancelReason;
import com.tcgshop.service.OrderCancellationService;
import com.tcgshop.service.ReturnRequestService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserOrderController {
	private final UserOrderService userOrderService;
	private final OrderCancellationService orderCancellationService;
	private final ReturnRequestService returnRequestService;

	public UserOrderController(UserOrderService userOrderService, OrderCancellationService orderCancellationService,
			ReturnRequestService returnRequestService) {
		this.userOrderService = userOrderService;
		this.orderCancellationService = orderCancellationService;
		this.returnRequestService = returnRequestService;
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

	@GetMapping("/orders/{id}/return")
	public String returnForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
		var order = userOrderService.getMyOrder(id);
		if (!order.canRequestReturn()) {
			redirectAttributes.addFlashAttribute("errorMessage", "\u0110\u01a1n h\u00e0ng n\u00e0y hi\u1ec7n kh\u00f4ng th\u1ec3 y\u00eau c\u1ea7u tr\u1ea3 h\u00e0ng");
			return "redirect:/orders/" + id;
		}
		model.addAttribute("order", order);
		model.addAttribute("returnForm", new CreateReturnRequest());
		model.addAttribute("returnReasons", ReturnReason.values());
		return "user/orders/return-form";
	}

	@PostMapping("/orders/{id}/return")
	public String createReturnRequest(@PathVariable Long id,
			@Valid @ModelAttribute("returnForm") CreateReturnRequest returnForm,
			BindingResult bindingResult, @RequestParam(name = "image", required = false) MultipartFile image,
			@RequestParam(name = "video", required = false) MultipartFile video, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("order", userOrderService.getMyOrder(id));
			model.addAttribute("returnReasons", ReturnReason.values());
			model.addAttribute("uploadReminder", "Vui l\u00f2ng ch\u1ecdn l\u1ea1i t\u1ec7p b\u1eb1ng ch\u1ee9ng sau khi s\u1eeda th\u00f4ng tin.");
			return "user/orders/return-form";
		}
		returnRequestService.createReturnRequest(id, returnForm, image, video);
		redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 g\u1eedi y\u00eau c\u1ea7u tr\u1ea3 h\u00e0ng. C\u1eeda h\u00e0ng s\u1ebd xem x\u00e9t v\u00e0 ph\u1ea3n h\u1ed3i");
		return "redirect:/orders/" + id;
	}

	@PostMapping("/orders/{id}/return/escalate")
	public String escalateReturn(@PathVariable Long id,
			@Valid @ModelAttribute("escalateForm") EscalateReturnRequest escalateForm,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
			return "redirect:/orders/" + id;
		}
		returnRequestService.escalate(id, escalateForm);
		redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 g\u1eedi khi\u1ebfu n\u1ea1i. Qu\u1ea3n l\u00fd s\u1ebd xem x\u00e9t v\u00e0 ph\u1ea3n h\u1ed3i");
		return "redirect:/orders/" + id;
	}
}
