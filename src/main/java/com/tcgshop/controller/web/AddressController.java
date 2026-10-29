package com.tcgshop.controller.web;

import com.tcgshop.config.AddressProperties;
import com.tcgshop.dto.request.AddressRequest;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/profile/addresses")
public class AddressController {
	private final AddressService addressService;
	private final AddressProperties addressProperties;
	private final RoleRedirectResolver roleRedirectResolver;

	public AddressController(AddressService addressService, AddressProperties addressProperties,
			RoleRedirectResolver roleRedirectResolver) {
		this.addressService = addressService;
		this.addressProperties = addressProperties;
		this.roleRedirectResolver = roleRedirectResolver;
	}

	@GetMapping
	public String list(Model model) {
		List<AddressResponse> addresses = addressService.list();
		model.addAttribute("addresses", addresses);
		model.addAttribute("addressCount", addresses.size());
		model.addAttribute("maxAddresses", addressProperties.maxPerUser());
		return "profile/addresses/index";
	}

	@GetMapping("/new")
	public String newAddress(@RequestParam(name = "returnUrl", required = false) String returnUrl,
			Model model, RedirectAttributes redirectAttributes) {
		List<AddressResponse> addresses = addressService.list();
		if (addresses.size() >= addressProperties.maxPerUser()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Bạn đã lưu đủ số lượng địa chỉ tối đa");
			return "redirect:/profile/addresses";
		}
		AddressRequest form = new AddressRequest();
		form.setMakeDefault(addresses.isEmpty());
		model.addAttribute("form", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("addressCount", addresses.size());
		model.addAttribute("maxAddresses", addressProperties.maxPerUser());
		model.addAttribute("defaultCheckboxDisabled", addresses.isEmpty());
		model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
		return "profile/addresses/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("form") AddressRequest form, BindingResult bindingResult,
			@RequestParam(name = "returnUrl", required = false) String returnUrl, Model model,
			RedirectAttributes redirectAttributes) {
		String safeReturnUrl = safeReturnUrl(returnUrl);
		if (bindingResult.hasErrors()) {
			List<AddressResponse> addresses = addressService.list();
			prepareFormModel(model, addresses.size(), false, false, safeReturnUrl);
			return "profile/addresses/form";
		}
		addressService.create(form);
		redirectAttributes.addFlashAttribute("successMessage", "Đã thêm địa chỉ");
		return "redirect:" + (safeReturnUrl == null ? "/profile/addresses" : safeReturnUrl);
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable Long id, Model model,
			@RequestParam(name = "returnUrl", required = false) String returnUrl) {
		AddressResponse address = addressService.get(id);
		List<AddressResponse> addresses = addressService.list();
		model.addAttribute("address", address);
		model.addAttribute("form", toRequest(address));
		prepareFormModel(model, addresses.size(), true, address.isDefault(), safeReturnUrl(returnUrl));
		return "profile/addresses/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AddressRequest form,
			BindingResult bindingResult, Model model,
			@RequestParam(name = "returnUrl", required = false) String returnUrl,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			AddressResponse address = addressService.get(id);
			model.addAttribute("address", address);
			prepareFormModel(model, addressService.list().size(), true, address.isDefault(),
					safeReturnUrl(returnUrl));
			return "profile/addresses/form";
		}
		addressService.update(id, form);
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật địa chỉ");
		return "redirect:/profile/addresses";
	}

	@PostMapping("/{id}/default")
	public String setDefault(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		addressService.setDefault(id);
		redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ mặc định");
		return "redirect:/profile/addresses";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		addressService.delete(id);
		redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ");
		return "redirect:/profile/addresses";
	}

	private void prepareFormModel(Model model, int addressCount, boolean isEdit,
			boolean currentDefault, String returnUrl) {
		model.addAttribute("isEdit", isEdit);
		model.addAttribute("addressCount", addressCount);
		model.addAttribute("maxAddresses", addressProperties.maxPerUser());
		model.addAttribute("currentDefault", currentDefault);
		model.addAttribute("defaultCheckboxDisabled", !isEdit && addressCount == 0 || isEdit && currentDefault);
		model.addAttribute("returnUrl", returnUrl);
	}

	private String safeReturnUrl(String returnUrl) {
		return roleRedirectResolver.isSafeReturnUrl(returnUrl) ? returnUrl : null;
	}

	private AddressRequest toRequest(AddressResponse address) {
		AddressRequest request = new AddressRequest();
		request.setReceiverName(address.receiverName());
		request.setPhoneNumber(address.phoneNumber());
		request.setProvince(address.province());
		request.setDistrict(address.district());
		request.setWard(address.ward());
		request.setDetailAddress(address.detailAddress());
		request.setMakeDefault(address.isDefault());
		return request;
	}
}
