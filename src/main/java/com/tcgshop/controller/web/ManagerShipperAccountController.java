package com.tcgshop.controller.web;

import com.tcgshop.dto.request.StaffAccountRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.StaffAccountResponse;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.service.StaffAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/shippers")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerShipperAccountController {
	private static final int PAGE_SIZE = 20;
	private static final String BASE_URL = "/manager/shippers";
	private static final String LIST_VIEW = "manager/shippers/list";
	private static final String FORM_VIEW = "manager/shippers/form";

	private final StaffAccountService staffAccountService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		PageRequest pageable = PageRequest.of(
				Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
		PageResponse<StaffAccountResponse> shippersPage =
				staffAccountService.searchShippers(keyword, status, pageable);
		model.addAttribute("shippersPage", shippersPage);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("statuses", UserStatus.values());
		addLayoutModel(model, "Quản lý Shipper");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String showCreateForm(Model model) {
		prepareForm(model);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(
			@Valid @ModelAttribute("staffAccountRequest") StaffAccountRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				staffAccountService.createShipper(request);
				redirectAttributes.addFlashAttribute(
						"successMessage", "Đã tạo tài khoản Shipper và gửi mật khẩu tạm qua email");
				return "redirect:" + BASE_URL;
			} catch (ConflictException | BusinessException exception) {
				result.reject("staffAccount.create.failed", exception.getMessage());
			}
		}
		prepareForm(model);
		return FORM_VIEW;
	}

	private void prepareForm(Model model) {
		if (!model.containsAttribute("staffAccountRequest")) {
			model.addAttribute("staffAccountRequest", new StaffAccountRequest());
		}
		addLayoutModel(model, "Tạo tài khoản Shipper");
	}

	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Shipper", "url", BASE_URL),
				Map.of("label", title, "url", BASE_URL)));
	}
}
