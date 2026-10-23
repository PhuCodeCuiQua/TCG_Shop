package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ShippingCarrierRequest;
import com.tcgshop.dto.response.AdminShippingCarrierResponse;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.ShippingService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shipping-carriers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminShippingCarrierController {

	private static final String BASE_URL = "/admin/shipping-carriers";
	private static final String LIST_VIEW = "admin/shipping-carriers/list";
	private static final String FORM_VIEW = "admin/shipping-carriers/form";

	private final ShippingService shippingService;

	@GetMapping
	public String list(Model model) {
		model.addAttribute("carriers", shippingService.getAllForAdmin());
		addLayoutModel(model, "Quản lý nhà vận chuyển");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("shippingCarrierRequest", new ShippingCarrierRequest());
		prepareForm(model, "Thêm nhà vận chuyển", BASE_URL);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(
			@Valid @ModelAttribute("shippingCarrierRequest") ShippingCarrierRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				shippingService.create(request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhà vận chuyển");
				return "redirect:" + BASE_URL;
			} catch (ConflictException | BusinessException ex) {
				result.reject("shippingCarrier.error", ex.getMessage());
			}
		}
		prepareForm(model, "Thêm nhà vận chuyển", BASE_URL);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		AdminShippingCarrierResponse carrier = shippingService.getForAdmin(id);
		ShippingCarrierRequest request = new ShippingCarrierRequest();
		request.setName(carrier.name());
		request.setCode(carrier.code());
		request.setBaseFee(carrier.baseFee());
		request.setFreeShipThreshold(carrier.freeShipThreshold());
		request.setActive(carrier.active());
		model.addAttribute("shippingCarrierRequest", request);
		prepareForm(model, "Cập nhật nhà vận chuyển", BASE_URL + "/" + id);
		model.addAttribute("carrier", carrier);
		return FORM_VIEW;
	}

	@PostMapping("/{id}")
	public String update(
			@PathVariable Long id,
			@Valid @ModelAttribute("shippingCarrierRequest") ShippingCarrierRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				shippingService.update(id, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật nhà vận chuyển");
				return "redirect:" + BASE_URL;
			} catch (ConflictException | BusinessException ex) {
				result.reject("shippingCarrier.error", ex.getMessage());
			}
		}
		prepareForm(model, "Cập nhật nhà vận chuyển", BASE_URL + "/" + id);
		model.addAttribute("carrier", shippingService.getForAdmin(id));
		return FORM_VIEW;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		boolean deleted = shippingService.removeOrHide(id);
		redirectAttributes.addFlashAttribute("successMessage", deleted
				? "Đã xóa nhà vận chuyển"
				: "Nhà vận chuyển đang được sử dụng hoặc còn logo chưa thể xóa an toàn nên đã được ẩn");
		return "redirect:" + BASE_URL;
	}

	private void prepareForm(Model model, String title, String formAction) {
		model.addAttribute("formAction", formAction);
		model.addAttribute("formTitle", title);
		addLayoutModel(model, title);
	}

	private void addLayoutModel(Model model, String title) {
		model.addAttribute("dashboardRole", "ADMIN");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản trị", "url", "/admin/dashboard"),
				Map.of("label", "Nhà vận chuyển", "url", BASE_URL)));
	}
}
