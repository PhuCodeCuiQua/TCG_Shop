package com.tcgshop.controller.web;

import com.tcgshop.dto.request.PlatformPromotionRequest;
import com.tcgshop.dto.request.PlatformPromotionScope;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PlatformPromotionService;
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
@RequestMapping({"/admin/promotions", "/manager/promotions"})
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PlatformPromotionController {

	private static final int PAGE_SIZE = 15;
	private static final String VIEW_ROOT = "manager/promotions";

	private final PlatformPromotionService promotionService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) PromotionType type,
			@RequestParam(required = false) ActiveStatus status,
			@RequestParam(required = false) Long shopId,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		String basePath = basePath();
		var pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("createdAt").descending());
		var promotionsPage = shopId == null
				? promotionService.search(type, status, pageable)
				: promotionService.search(type, status, shopId, pageable);
		LocalDateTime now = LocalDateTime.now();
		Set<Long> expiredPromotionIds = promotionsPage.content().stream()
				.filter(promotion -> promotion.endDate() != null && promotion.endDate().isBefore(now))
				.map(PlatformPromotionResponse::id)
				.collect(Collectors.toSet());
		model.addAttribute("promotionsPage", promotionsPage);
		model.addAttribute("expiredPromotionIds", expiredPromotionIds);
		model.addAttribute("promotionTypes", PromotionType.values());
		model.addAttribute("activeStatuses", ActiveStatus.values());
		model.addAttribute("selectedType", type);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("selectedShopId", shopId);
		model.addAttribute("shopOptions", promotionService.getApprovedShopOptions());
		model.addAttribute("couponCreatePath", currentUser.hasRole(RoleName.ROLE_ADMIN)
				? "/admin/platform-coupons/new"
				: "/manager/platform-coupons/new");
		addLayoutModel(model, basePath, "Khuyến mãi hệ thống", "Khuyến mãi");
		return VIEW_ROOT + "/list";
	}

	@GetMapping("/new")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String createForm(Model model) {
		prepareForm(model, new PlatformPromotionRequest(), null, null);
		return VIEW_ROOT + "/form";
	}

	@GetMapping("/{promotionId}/edit")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String editForm(@PathVariable Long promotionId, Model model) {
		PlatformPromotionResponse promotion = promotionService.getById(promotionId);
		PlatformPromotionRequest request = new PlatformPromotionRequest();
		request.setTitle(promotion.title());
		request.setType(promotion.type());
		request.setDiscountPercent(promotion.discountPercent());
		request.setDiscountAmount(promotion.discountAmount());
		request.setMinOrderValue(promotion.minOrderValue());
		request.setGameId(promotion.gameId());
		request.setSeriesId(promotion.seriesId());
		request.setProductIds(promotion.productIds() == null ? List.of() : promotion.productIds());
		request.setScope(promotion.gameId() != null || promotion.seriesId() != null
				? PlatformPromotionScope.GAME_SERIES
				: promotion.productIds() != null && !promotion.productIds().isEmpty()
						? PlatformPromotionScope.PRODUCTS
						: PlatformPromotionScope.ALL);
		request.setStartDate(promotion.startDate());
		request.setEndDate(promotion.endDate());
		request.setStatus(promotion.status());
		prepareForm(model, request, promotionId, null);
		return VIEW_ROOT + "/form";
	}

	@PostMapping("/new")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String create(@Valid @ModelAttribute("promotionRequest") PlatformPromotionRequest request,
			BindingResult result, Model model, RedirectAttributes redirectAttributes) {
		return save(null, request, result, model, redirectAttributes);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and #promotionId != null)")
	public String save(
			@RequestParam(required = false) Long promotionId,
			@Valid @ModelAttribute("promotionRequest") PlatformPromotionRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				promotionService.save(promotionId, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã lưu chương trình khuyến mãi");
				return "redirect:" + basePath();
			} catch (BadRequestException | ConflictException exception) {
				result.reject("promotion.invalid", exception.getMessage());
			}
		}
		prepareForm(model, request, promotionId, result);
		return VIEW_ROOT + "/form";
	}

	@PostMapping("/{promotionId}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String updateStatus(
			@PathVariable Long promotionId,
			@RequestParam ActiveStatus status,
			RedirectAttributes redirectAttributes) {
		promotionService.setStatus(promotionId, status);
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái khuyến mãi");
		return "redirect:" + basePath();
	}

	private void prepareForm(Model model, PlatformPromotionRequest request, Long promotionId, BindingResult result) {
		model.addAttribute("promotionRequest", request);
		model.addAttribute("promotionId", promotionId);
		model.addAttribute("formTitle", promotionId == null ? "Tạo khuyến mãi hệ thống" : "Sửa khuyến mãi hệ thống");
		model.addAttribute("formAction", basePath() + (promotionId == null ? "/new" : ""));
		model.addAttribute("promotionTypes", PromotionType.values());
		model.addAttribute("promotionScopes", PlatformPromotionScope.values());
		model.addAttribute("activeStatuses", ActiveStatus.values());
		model.addAttribute("gameOptions", promotionService.getGameOptions());
		model.addAttribute("seriesOptions", promotionService.getSeriesOptions());
		model.addAttribute("productOptions", promotionService.getSelectedProductOptions(request.getProductIds()));
		model.addAttribute("shopOptions", promotionService.getApprovedShopOptions());
		if (result != null) {
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "promotionRequest", result);
			model.addAttribute("globalErrors", result.getGlobalErrors());
		}
		addLayoutModel(model, basePath(), "Khuyến mãi hệ thống", "Biểu mẫu");
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
		return currentUser.hasRole(RoleName.ROLE_ADMIN) ? "/admin/promotions" : "/manager/promotions";
	}
}
