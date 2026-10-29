package com.tcgshop.controller.web;

import com.tcgshop.dto.request.PlatformCouponRequest;
import com.tcgshop.dto.response.PlatformCouponResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PlatformCouponAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
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
@RequestMapping({"/admin/platform-coupons", "/manager/platform-coupons"})
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PlatformCouponAdminController {

	private static final int PAGE_SIZE = 15;
	private static final String VIEW_ROOT = "manager/platform-coupons";

	private final PlatformCouponAdminService couponAdminService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) ActiveStatus status,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		String basePath = basePath();
		var couponsPage = couponAdminService.search(
				status, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("createdAt").descending()));
		LocalDateTime now = LocalDateTime.now();
		Set<Long> expiredCouponIds = couponsPage.content().stream()
				.filter(coupon -> coupon.endDate() != null && coupon.endDate().isBefore(now))
				.map(PlatformCouponResponse::id)
				.collect(Collectors.toSet());
		model.addAttribute("couponsPage", couponsPage);
		model.addAttribute("expiredCouponIds", expiredCouponIds);
		model.addAttribute("activeStatuses", ActiveStatus.values());
		model.addAttribute("selectedStatus", status);
		addLayoutModel(model, basePath, "Mã giảm giá hệ thống", "Mã giảm giá");
		return VIEW_ROOT + "/list";
	}

	@GetMapping("/new")
	@PreAuthorize("hasRole('ADMIN')")
	public String createForm(Model model) {
		prepareForm(model, new PlatformCouponRequest(), null, null);
		return VIEW_ROOT + "/form";
	}

	@GetMapping("/{couponId}/edit")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String editForm(@PathVariable Long couponId, Model model) {
		PlatformCouponResponse coupon = couponAdminService.getById(couponId);
		PlatformCouponRequest request = new PlatformCouponRequest();
		request.setCode(coupon.code());
		request.setDiscountType(coupon.discountType());
		request.setDiscountValue(coupon.discountValue());
		request.setMinOrderValue(coupon.minOrderValue());
		request.setMaxDiscountAmount(coupon.maxDiscountAmount());
		request.setTotalQuantity(coupon.totalQuantity());
		request.setLimitPerUser(coupon.limitPerUser());
		request.setStartDate(coupon.startDate());
		request.setEndDate(coupon.endDate());
		request.setStatus(coupon.status());
		prepareForm(model, request, couponId, null);
		return VIEW_ROOT + "/form";
	}

	@PostMapping("/new")
	@PreAuthorize("hasRole('ADMIN')")
	public String create(@Valid @ModelAttribute("couponRequest") PlatformCouponRequest request,
			BindingResult result, Model model, RedirectAttributes redirectAttributes) {
		return save(null, request, result, model, redirectAttributes);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and #couponId != null)")
	public String save(
			@RequestParam(required = false) Long couponId,
			@Valid @ModelAttribute("couponRequest") PlatformCouponRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				couponAdminService.save(couponId, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá hệ thống");
				return "redirect:" + basePath();
			} catch (BadRequestException | ConflictException exception) {
				result.reject("coupon.invalid", exception.getMessage());
			}
		}
		prepareForm(model, request, couponId, result);
		return VIEW_ROOT + "/form";
	}

	@PostMapping("/{couponId}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String updateStatus(
			@PathVariable Long couponId,
			@RequestParam ActiveStatus status,
			RedirectAttributes redirectAttributes) {
		couponAdminService.setStatus(couponId, status);
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mã giảm giá");
		return "redirect:" + basePath();
	}

	private void prepareForm(Model model, PlatformCouponRequest request, Long couponId, BindingResult result) {
		model.addAttribute("couponRequest", request);
		model.addAttribute("couponId", couponId);
		model.addAttribute("formTitle", couponId == null ? "Tạo mã giảm giá hệ thống" : "Sửa mã giảm giá hệ thống");
		model.addAttribute("formAction", basePath() + (couponId == null ? "/new" : ""));
		model.addAttribute("discountTypes", DiscountType.values());
		model.addAttribute("activeStatuses", ActiveStatus.values());
		if (result != null) {
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "couponRequest", result);
			model.addAttribute("globalErrors", result.getGlobalErrors());
		}
		addLayoutModel(model, basePath(), "Mã giảm giá hệ thống", "Biểu mẫu");
	}

	private void addLayoutModel(Model model, String basePath, String title, String breadcrumb) {
		model.addAttribute("dashboardRole", currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", basePath);
		model.addAttribute("basePath", basePath);
		model.addAttribute("canManage", currentUser.hasRole(RoleName.ROLE_ADMIN) || currentUser.hasRole(RoleName.ROLE_MANAGER));
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản trị", "url", currentUser.hasRole(RoleName.ROLE_ADMIN) ? "/admin/dashboard" : "/manager/dashboard"),
				Map.of("label", title, "url", basePath),
				Map.of("label", breadcrumb, "url", basePath)));
	}

	private String basePath() {
		return currentUser.hasRole(RoleName.ROLE_ADMIN) ? "/admin/platform-coupons" : "/manager/platform-coupons";
	}
}
