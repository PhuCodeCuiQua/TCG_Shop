package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorCouponRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.VendorCouponResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.VendorCouponService;
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
@RequestMapping("/vendor/coupons")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorCouponController {
	private static final int PAGE_SIZE = 10;
	private static final String LIST_VIEW = "vendor/coupons";
	private static final String FORM_VIEW = "vendor/coupon-form";

	private final VendorCouponService couponService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		PageResponse<VendorCouponResponse> coupons = couponService.getCurrentShopCoupons(
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
		model.addAttribute("couponsPage", coupons);
		prepareLayout(model, "Mã giảm giá", "/vendor/coupons");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		if (!model.containsAttribute("couponRequest")) model.addAttribute("couponRequest", new VendorCouponRequest());
		prepareForm(model, null);
		return FORM_VIEW;
	}

	@PostMapping("/new")
	public String create(@Valid @ModelAttribute("couponRequest") VendorCouponRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		return save(null, request, bindingResult, model, redirectAttributes);
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		VendorCouponResponse coupon = couponService.getCurrentShopCoupon(id);
		if (!model.containsAttribute("couponRequest")) {
			VendorCouponRequest request = new VendorCouponRequest();
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
			model.addAttribute("couponRequest", request);
		}
		prepareForm(model, id);
		return FORM_VIEW;
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("couponRequest") VendorCouponRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		return save(id, request, bindingResult, model, redirectAttributes);
	}

	@PostMapping("/{id}/toggle")
	public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		VendorCouponResponse result = couponService.toggleCurrentShopCoupon(id);
		redirectAttributes.addFlashAttribute("successMessage",
				result.status() == ActiveStatus.ACTIVE ? "Đã bật mã giảm giá" : "Đã tạm dừng mã giảm giá");
		return "redirect:/vendor/coupons";
	}

	@PostMapping("/{id}/delete")
	public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		couponService.deactivateCurrentShopCoupon(id);
		redirectAttributes.addFlashAttribute("successMessage", "Mã giảm giá đã được chuyển sang trạng thái tạm dừng");
		return "redirect:/vendor/coupons";
	}

	private String save(Long couponId, VendorCouponRequest request, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasErrors()) {
			try {
				couponService.saveCurrentShopCoupon(couponId, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá");
				return "redirect:/vendor/coupons";
			} catch (BadRequestException exception) {
				if (exception.getMessage().contains("Thời gian kết thúc")) {
					bindingResult.rejectValue("endDate", "coupon.dateRange", exception.getMessage());
				} else {
					bindingResult.reject("coupon.invalid", exception.getMessage());
				}
			} catch (ConflictException exception) {
				bindingResult.rejectValue("code", "coupon.duplicate", exception.getMessage());
			}
		}
		prepareForm(model, couponId);
		return FORM_VIEW;
	}

	private void prepareForm(Model model, Long couponId) {
		model.addAttribute("couponId", couponId);
		model.addAttribute("couponAction", couponId == null ? "/vendor/coupons/new" : "/vendor/coupons/" + couponId + "/edit");
		model.addAttribute("discountTypes", DiscountType.values());
		model.addAttribute("activeStatuses", ActiveStatus.values());
		model.addAttribute("globalErrors", model.containsAttribute("org.springframework.validation.BindingResult.couponRequest")
				? ((BindingResult) model.getAttribute("org.springframework.validation.BindingResult.couponRequest")).getGlobalErrors()
				: List.of());
		prepareLayout(model, couponId == null ? "Tạo mã giảm giá" : "Sửa mã giảm giá", "/vendor/coupons");
	}

	private void prepareLayout(Model model, String title, String currentPath) {
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Mã giảm giá", "url", "/vendor/coupons"),
				Map.of("label", title, "url", currentPath)));
	}
}
