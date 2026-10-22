package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ChangePasswordRequest;
import com.tcgshop.dto.response.AuthTokensResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {
	private final AuthService authService;
	private final CookieUtil cookieUtil;
	private final RoleRedirectResolver roleRedirectResolver;

	public ProfileController(AuthService authService, CookieUtil cookieUtil,
			RoleRedirectResolver roleRedirectResolver) {
		this.authService = authService;
		this.cookieUtil = cookieUtil;
		this.roleRedirectResolver = roleRedirectResolver;
	}

	@GetMapping("/change-password")
	public String showChangePasswordForm(Model model) {
		model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
		return "profile/change-password";
	}

	@PostMapping("/change-password")
	public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
			BindingResult bindingResult, Model model, HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			// Form lỗi không được trả lại mật khẩu hiện tại, mật khẩu mới hoặc giá trị xác nhận.
			request.setCurrentPassword(null);
			request.setNewPassword(null);
			request.setConfirmPassword(null);
			BeanPropertyBindingResult safeResult = new BeanPropertyBindingResult(request, "changePasswordRequest");
			for (ObjectError error : bindingResult.getAllErrors()) {
				if (error instanceof FieldError fieldError) {
					safeResult.addError(new FieldError("changePasswordRequest", fieldError.getField(), null,
							fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
							fieldError.getDefaultMessage()));
				} else {
					safeResult.addError(error);
				}
			}
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "changePasswordRequest", safeResult);
			return "profile/change-password";
		}

		AuthTokensResponse tokens = authService.changePassword(request);
		cookieUtil.addAccessTokenCookie(response, tokens.accessToken());
		if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
			cookieUtil.addRefreshTokenCookie(response, tokens.refreshToken());
		}
		redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công");
		return "redirect:" + roleRedirectResolver.resolveTargetUrl(toAuthorities(tokens.roles()));
	}

	private Collection<? extends GrantedAuthority> toAuthorities(Collection<RoleName> roles) {
		return roles.stream().map(RoleName::name).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
	}
}
