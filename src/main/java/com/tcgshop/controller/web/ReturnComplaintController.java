package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ReturnComplaintDecisionRequest;
import com.tcgshop.dto.request.ReturnRefundRequest;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.service.ReturnComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/returns")
public class ReturnComplaintController {
	private static final int PAGE_SIZE = 10;
	private final ReturnComplaintService service;

	public ReturnComplaintController(ReturnComplaintService service) {
		this.service = service;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "complaints") String tab,
			@RequestParam(defaultValue = "0") int page, Model model) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
		boolean refunds = "refunds".equals(tab);
		model.addAttribute("tab", refunds ? "refunds" : "complaints");
		model.addAttribute("items", refunds ? service.listRefundable(pageable) : service.listComplaints(pageable));
		return "manager/returns/list";
	}

	@GetMapping("/{orderId}")
	public String detail(@PathVariable Long orderId, Model model) {
		model.addAttribute("detail", service.getDetail(orderId));
		return "manager/returns/detail";
	}

	@PostMapping("/{orderId}/accept")
	public String accept(@PathVariable Long orderId, @Valid @ModelAttribute ReturnComplaintDecisionRequest request,
			BindingResult errors, RedirectAttributes ra) {
		return handle(orderId, errors, ra, "Đã chấp nhận khiếu nại", () -> service.acceptComplaint(orderId, request.note()));
	}

	@PostMapping("/{orderId}/reject")
	public String reject(@PathVariable Long orderId, @Valid @ModelAttribute ReturnComplaintDecisionRequest request,
			BindingResult errors, RedirectAttributes ra) {
		return handle(orderId, errors, ra, "Đã từ chối khiếu nại", () -> service.rejectComplaint(orderId, request.note()));
	}

	@PostMapping("/{orderId}/refund")
	public String refund(@PathVariable Long orderId, @Valid @ModelAttribute ReturnRefundRequest request,
			BindingResult errors, RedirectAttributes ra) {
		return handle(orderId, errors, ra, "Đã ghi nhận hoàn tiền", () -> service.refund(orderId, request.note()));
	}

	private String handle(Long orderId, BindingResult errors, RedirectAttributes ra, String success, Runnable action) {
		if (errors.hasErrors()) {
			ra.addFlashAttribute("errorMessage", errors.getFieldError().getDefaultMessage());
		} else {
			try {
				action.run();
				ra.addFlashAttribute("successMessage", success);
			} catch (BusinessException | BadRequestException e) {
				ra.addFlashAttribute("errorMessage", e.getMessage());
			}
		}
		return "redirect:/manager/returns/" + orderId;
	}
}