package com.tcgshop.controller.web;

import com.tcgshop.dto.request.StaffAccountRequest;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.StaffAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/managers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminManagerAccountController {
	private static final String BASE_URL = "/admin/managers";
	private static final String FORM_VIEW = "admin/managers/form";

	private final StaffAccountService staffAccountService;

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
				staffAccountService.createManager(request);
				redirectAttributes.addFlashAttribute(
						"successMessage", "Đã tạo tài khoản Manager và gửi mật khẩu tạm qua email");
				return "redirect:" + BASE_URL + "/new";
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
		model.addAttribute("dashboardRole", "ADMIN");
		model.addAttribute("dashboardTitle", "Tạo tài khoản Manager");
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản trị", "url", "/admin/dashboard"),
				Map.of("label", "Tạo Manager", "url", BASE_URL + "/new")));
	}
}
