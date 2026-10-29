package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ShipperAssignRequest;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShipperAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Phân công shipper cho đơn CONFIRMED (PA-20). Manager và Admin dùng chung nên URL nằm dưới /manager/**.
 * Người thao tác lấy từ SecurityContext trong Service, không nhận từ client.
 */
@Controller
@RequestMapping("/manager/shipper-assignment")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class ShipperAssignmentController {

	private static final int PAGE_SIZE = 10;
	private static final String BASE_URL = "/manager/shipper-assignment";

	private final ShipperAssignmentService shipperAssignmentService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(defaultValue = "false") boolean assigned,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("createdAt").ascending());
		model.addAttribute("ordersPage", shipperAssignmentService.searchOrders(assigned, keyword, pageable));
		model.addAttribute("shippers", shipperAssignmentService.getShipperLoads());
		// Giữ tab và từ khóa trên URL khi chuyển trang (mục 13)
		model.addAttribute("assigned", assigned);
		model.addAttribute("keyword", keyword);
		addLayoutModel(model);
		return "manager/shipper-assignment/list";
	}

	@PostMapping("/{orderId}/assign")
	public String assign(
			@PathVariable Long orderId,
			@RequestParam(defaultValue = "false") boolean assigned,
			@Valid @ModelAttribute ShipperAssignRequest request,
			BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					result.getAllErrors().get(0).getDefaultMessage());
			return redirectToList(assigned);
		}
		try {
			shipperAssignmentService.assign(orderId, request.shipperId());
			redirectAttributes.addFlashAttribute("successMessage", "Đã phân công shipper cho đơn hàng");
		} catch (RuntimeException ex) {
			// Chỉ lỗi nghiệp vụ hiện bằng toast; lỗi khác (404, 403, 409...) do GlobalExceptionHandler xử lý
			if (!isUserFacing(ex)) {
				throw ex;
			}
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToList(assigned);
	}

	private boolean isUserFacing(RuntimeException ex) {
		return ex instanceof BadRequestException
				|| ex instanceof BusinessException
				|| ex instanceof ConflictException;
	}

	private String redirectToList(boolean assigned) {
		return "redirect:" + BASE_URL + "?assigned=" + assigned;
	}

	private void addLayoutModel(Model model) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", "Phân công shipper");
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Phân công shipper", "url", BASE_URL)));
	}
}