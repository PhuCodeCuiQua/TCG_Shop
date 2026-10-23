package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorProductEditRequest;
import com.tcgshop.dto.response.VendorProductEditResponse;
import com.tcgshop.service.VendorProductManageService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorProductEditController {
	private final VendorProductManageService productManageService;

	@GetMapping("/vendor/products/{id}/edit")
	public String showEditForm(@PathVariable Long id, Model model) {
		VendorProductEditResponse product = productManageService.getOwnedProduct(id);
		if (!model.containsAttribute("productRequest")) {
			VendorProductEditRequest request = new VendorProductEditRequest();
			request.setPrice(product.price());
			request.setStock(product.stock());
			request.setDescription(product.description());
			model.addAttribute("productRequest", request);
		}
		prepareModel(model, product);
		return "vendor/product-edit";
	}

	@PostMapping("/vendor/products/{id}/edit")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("productRequest") VendorProductEditRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		VendorProductEditResponse product = productManageService.getOwnedProduct(id);
		if (bindingResult.hasErrors()) {
			prepareModel(model, product);
			return "vendor/product-edit";
		}
		productManageService.updateOwnedProduct(id, request);
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm.");
		return "redirect:/vendor/products";
	}

	private void prepareModel(Model model, VendorProductEditResponse product) {
		model.addAttribute("product", product);
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Sửa sản phẩm");
		model.addAttribute("currentPath", "/vendor/products");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Sản phẩm", "url", "/vendor/products"),
				Map.of("label", "Sửa sản phẩm", "url", "/vendor/products/" + product.id() + "/edit")));
	}
}
