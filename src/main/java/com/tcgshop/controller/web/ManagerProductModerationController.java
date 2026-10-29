package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ProductBanRequest;
import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ProductModerationService;
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
@RequestMapping("/manager/products")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerProductModerationController {

	private static final int PAGE_SIZE = 20;
	private static final String BASE_URL = "/manager/products";
	private static final String LIST_VIEW = "manager/products/list";
	private static final String DETAIL_VIEW = "manager/products/detail";

	private final ProductModerationService productModerationService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) Long shopId,
			@RequestParam(required = false) ProductType type,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		model.addAttribute("productsPage", productModerationService.search(
				shopId, type, status, keyword,
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))));
		model.addAttribute("shopOptions", productModerationService.getShopOptions());
		model.addAttribute("productTypes", ProductType.values());
		model.addAttribute("productStatuses", ProductStatus.values());
		model.addAttribute("selectedShopId", shopId);
		model.addAttribute("selectedType", type);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("keyword", keyword);
		addLayoutModel(model, "Quản lý sản phẩm");
		return LIST_VIEW;
	}

	@GetMapping("/{productId}")
	public String detail(@PathVariable Long productId, Model model) {
		prepareDetail(model, productModerationService.getById(productId));
		return DETAIL_VIEW;
	}

	@PostMapping("/{productId}/ban")
	public String ban(
			@PathVariable Long productId,
			@Valid @ModelAttribute("productBanRequest") ProductBanRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			prepareDetail(model, productModerationService.getById(productId));
			return DETAIL_VIEW;
		}
		try {
			productModerationService.ban(productId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ sản phẩm và thông báo cho chủ cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + productId;
	}

	@PostMapping("/{productId}/restore")
	public String restore(@PathVariable Long productId, RedirectAttributes redirectAttributes) {
		try {
			productModerationService.restore(productId);
			redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm và thông báo cho chủ cửa hàng");
		} catch (ConflictException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:" + BASE_URL + "/" + productId;
	}

	private void prepareDetail(Model model, ProductModerationResponse product) {
		model.addAttribute("product", product);
		if (!model.containsAttribute("productBanRequest")) {
			model.addAttribute("productBanRequest", new ProductBanRequest());
		}
		addLayoutModel(model, "Chi tiết sản phẩm");
	}

	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Sản phẩm", "url", BASE_URL),
				Map.of("label", title, "url", BASE_URL)));
	}
}
