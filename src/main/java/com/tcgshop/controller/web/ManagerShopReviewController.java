package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ShopLockRequest;
import com.tcgshop.dto.request.ShopProfileUpdateRequest;
import com.tcgshop.dto.request.ShopRejectRequest;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShopReviewService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/shops")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerShopReviewController {
	private static final int PAGE_SIZE = 20;
	private static final String BASE_URL = "/manager/shops";
	private static final String LIST_VIEW = "manager/shops/list";
	private static final String DETAIL_VIEW = "manager/shops/detail";
	private static final String EDIT_VIEW = "manager/shops/form";

	private final ShopReviewService shopReviewService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) ShopStatus status,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		model.addAttribute("shopsPage", shopReviewService.search(
				keyword, status,
				PageRequest.of(Math.max(page, 0), PAGE_SIZE,
						Sort.by(Sort.Direction.DESC, "createdAt"))));
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("statuses", ShopStatus.values());
		addLayoutModel(model, "Duyệt cửa hàng");
		return LIST_VIEW;
	}

	@GetMapping("/{shopId}")
	public String detail(@PathVariable Long shopId, Model model) {
		prepareDetail(model, shopReviewService.getById(shopId));
		return DETAIL_VIEW;
	}

	@GetMapping("/{shopId}/edit")
	public String edit(@PathVariable Long shopId, Model model) {
		prepareEdit(model, shopReviewService.getById(shopId), null);
		return EDIT_VIEW;
	}

	@PostMapping("/{shopId}/edit")
	public String updateProfile(
			@PathVariable Long shopId,
			@Valid @ModelAttribute("shopProfileRequest") ShopProfileUpdateRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			prepareEdit(model, shopReviewService.getById(shopId), request);
			return EDIT_VIEW;
		}

		try {
			shopReviewService.updateProfile(shopId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin chi nhánh");
			return "redirect:" + BASE_URL + "/" + shopId;
		} catch (ConflictException exception) {
			result.rejectValue("name", "duplicate", exception.getMessage());
			prepareEdit(model, shopReviewService.getById(shopId), request);
			return EDIT_VIEW;
		}
	}

	@PostMapping("/{shopId}/approve")
	public String approve(@PathVariable Long shopId, RedirectAttributes redirectAttributes) {
		try {
			shopReviewService.approve(shopId);
			redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + shopId;
	}

	@PostMapping("/{shopId}/reject")
	public String reject(
			@PathVariable Long shopId,
			@Valid @ModelAttribute("shopRejectRequest") ShopRejectRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			prepareDetail(model, shopReviewService.getById(shopId));
			return DETAIL_VIEW;
		}
		try {
			shopReviewService.reject(shopId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối hồ sơ cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + shopId;
	}

	@PostMapping("/{shopId}/suspend")
	public String suspend(
			@PathVariable Long shopId,
			@Valid @ModelAttribute("shopLockRequest") ShopLockRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			prepareDetail(model, shopReviewService.getById(shopId));
			return DETAIL_VIEW;
		}
		try {
			shopReviewService.suspend(shopId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã tạm ngừng cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + shopId;
	}

	@PostMapping("/{shopId}/reactivate")
	public String reactivate(@PathVariable Long shopId, RedirectAttributes redirectAttributes) {
		try {
			shopReviewService.reactivate(shopId);
			redirectAttributes.addFlashAttribute("successMessage", "Đã mở lại cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + shopId;
	}

	private void prepareDetail(Model model, com.tcgshop.dto.response.ShopReviewResponse shop) {
		model.addAttribute("shop", shop);
		if (!model.containsAttribute("shopRejectRequest")) {
			model.addAttribute("shopRejectRequest", new ShopRejectRequest());
		}
		if (!model.containsAttribute("shopLockRequest")) {
			model.addAttribute("shopLockRequest", new ShopLockRequest());
		}
		addLayoutModel(model, "Chi tiết cửa hàng");
	}

	private void prepareEdit(
			Model model,
			com.tcgshop.dto.response.ShopReviewResponse shop,
			ShopProfileUpdateRequest request) {
		model.addAttribute("shop", shop);
		if (!model.containsAttribute("shopProfileRequest")) {
			ShopProfileUpdateRequest form = request;
			if (form == null) {
				form = new ShopProfileUpdateRequest();
				form.setName(shop.name());
				form.setPhoneNumber(shop.phoneNumber());
				form.setPickupAddress(shop.pickupAddress());
				form.setDescription(shop.description());
			}
			model.addAttribute("shopProfileRequest", form);
		}
		addLayoutModel(model, "Sửa thông tin chi nhánh");
	}

	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Cửa hàng", "url", BASE_URL),
				Map.of("label", title, "url", BASE_URL)));
	}
}
