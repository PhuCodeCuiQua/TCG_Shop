package com.tcgshop.controller.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.request.ForgotPasswordRequest;
import com.tcgshop.dto.request.ResetPasswordRequest;
import com.tcgshop.dto.response.AuthTokensResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.AccountNotActivatedException;
import com.tcgshop.exception.AuthException;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.RoleRedirectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.tcgshop.config.OtpProperties;
import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;
import com.tcgshop.service.AuthService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	private final OtpProperties otpProperties;
	private final CookieUtil cookieUtil;
	private final RoleRedirectResolver roleRedirectResolver;

	public AuthController(AuthService authService, OtpProperties otpProperties,
			CookieUtil cookieUtil, RoleRedirectResolver roleRedirectResolver) {
		this.authService = authService;
		this.otpProperties = otpProperties;
		this.cookieUtil = cookieUtil;
		this.roleRedirectResolver = roleRedirectResolver;
	}

	@GetMapping("/login")
	public String showLoginForm(@RequestParam(required = false) String returnUrl, Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			if (authentication.getPrincipal() instanceof CustomUserDetails user
					&& user.isMustChangePassword()) {
				return "redirect:/profile/change-password";
			}
			if (roleRedirectResolver.isSafeReturnUrl(returnUrl)) {
				return "redirect:" + returnUrl;
			}
			return "redirect:" + roleRedirectResolver.resolveTargetUrl(authentication.getAuthorities());
		}
		LoginRequest request = new LoginRequest();
		request.setReturnUrl(returnUrl);
		model.addAttribute("loginRequest", request);
		return "auth/login";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
			HttpServletResponse response) {
		if (bindingResult.hasErrors()) {
			return loginValidationError(request, bindingResult, model);
		}

		try {
			AuthTokensResponse tokens = authService.login(request);
			setAuthCookies(response, tokens);
			if (tokens.mustChangePassword()) {
				return "redirect:/profile/change-password";
			}
			if (roleRedirectResolver.isSafeReturnUrl(request.getReturnUrl())) {
				return "redirect:" + request.getReturnUrl();
			}
			return "redirect:" + roleRedirectResolver.resolveTargetUrl(toAuthorities(tokens.roles()));
		} catch (AccountNotActivatedException exception) {
			// Ngoại lệ có chủ đích: chỉ luồng đăng nhập đổi trạng thái INACTIVE thành hướng dẫn nhập OTP.
			redirectAttributes.addFlashAttribute("successMessage",
					"Tài khoản chưa được kích hoạt, vui lòng nhập mã OTP");
			return "redirect:/auth/verify-otp?email=" + encodeEmail(exception.getEmail());
		} catch (AuthException exception) {
			// Chỉ bắt lỗi xác thực tại đây để giữ lại email/returnUrl; tuyệt đối không giữ mật khẩu trong model.
			request.setPassword(null);
			model.addAttribute("loginRequest", request);
			model.addAttribute("errorMessage", exception.getMessage());
			return "auth/login";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		authService.logout(cookieUtil.readRefreshToken(request).orElse(null));
		cookieUtil.clearAuthCookies(response);
		redirectAttributes.addFlashAttribute("successMessage", "Bạn đã đăng xuất");
		return "redirect:/auth/login";
	}

	@GetMapping("/register")
	public String showRegisterForm(Model model) {
		model.addAttribute("registerRequest", new RegisterRequest());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(
			@Valid @ModelAttribute("registerRequest") RegisterRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			// Không bao giờ đưa lại mật khẩu đã gửi lên vào model sau lỗi validation.
			request.setPassword(null);
			request.setConfirmPassword(null);
			BeanPropertyBindingResult safeBindingResult = new BeanPropertyBindingResult(request, "registerRequest");
			for (ObjectError error : bindingResult.getAllErrors()) {
				if (error instanceof FieldError fieldError
						&& ("password".equals(fieldError.getField()) || "confirmPassword".equals(fieldError.getField()))) {
					safeBindingResult.addError(new FieldError(
							"registerRequest", fieldError.getField(), null, fieldError.isBindingFailure(),
							fieldError.getCodes(), fieldError.getArguments(), fieldError.getDefaultMessage()));
				} else {
					safeBindingResult.addError(error);
				}
			}
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registerRequest", safeBindingResult);
			return "auth/register";
		}

		authService.register(request);
		redirectAttributes.addFlashAttribute("successMessage", "Mã xác nhận đã được gửi tới email của bạn");
		return "redirect:/auth/verify-otp?email=" + encodeEmail(request.getEmail());
	}

	@GetMapping("/verify-otp")
	public String showVerifyOtpForm(@RequestParam(required = false) String email, Model model) {
		VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
		verifyRequest.setEmail(email);
		model.addAttribute("verifyOtpRequest", verifyRequest);
		model.addAttribute("resendOtpRequest", resendRequest(email));
		model.addAttribute("validMinutes", otpProperties.validMinutes());
		return "auth/verify-otp";
	}

	@PostMapping("/verify-otp")
	public String verifyOtp(
			@Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			request.setOtp(null);
			model.addAttribute("resendOtpRequest", resendRequest(request.getEmail()));
			model.addAttribute("validMinutes", otpProperties.validMinutes());
			return "auth/verify-otp";
		}

		authService.verifyRegistration(request);
		redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt thành công, vui lòng đăng nhập");
		return "redirect:/auth/login";
	}

	@PostMapping("/resend-otp")
	public String resendOtp(
			@Valid @ModelAttribute("resendOtpRequest") ResendOtpRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
			verifyRequest.setEmail(request.getEmail());
			model.addAttribute("verifyOtpRequest", verifyRequest);
			model.addAttribute("validMinutes", otpProperties.validMinutes());
			return "auth/verify-otp";
		}

		authService.resendRegistrationOtp(request);
		// Phản hồi giống nhau dù email có tài khoản hay không để tránh dò tìm email đã đăng ký.
		redirectAttributes.addFlashAttribute("successMessage", "Nếu tài khoản cần xác thực, mã sẽ được gửi tới email");
		return "redirect:/auth/verify-otp?email=" + encodeEmail(request.getEmail());
	}

	@GetMapping("/forgot-password")
	public String showForgotPasswordForm(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			return "redirect:/profile/change-password";
		}
		model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
		return "auth/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String requestPasswordReset(
			@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "forgotPasswordRequest", bindingResult);
			return "auth/forgot-password";
		}

		authService.requestPasswordReset(request);
		redirectAttributes.addFlashAttribute("successMessage",
				"Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi. Vui lòng kiểm tra hộp thư (cả thư mục spam)");
		return "redirect:/auth/reset-password?email=" + encodeEmail(request.getEmail());
	}

	@GetMapping("/reset-password")
	public String showResetPasswordForm(@RequestParam(required = false) String email, Model model) {
		if (email == null || email.isBlank()) {
			return "redirect:/auth/forgot-password";
		}
		ResetPasswordRequest request = new ResetPasswordRequest();
		request.setEmail(email);
		model.addAttribute("resetPasswordRequest", request);
		return "auth/reset-password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(
			@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpServletRequest servletRequest) {
		if (bindingResult.hasErrors()) {
			sanitizeResetPasswordRequest(request, bindingResult, model);
			return "auth/reset-password";
		}

		try {
			authService.resetPassword(request);
		} catch (BadRequestException exception) {
			// Với Referer hợp lệ, để GlobalExceptionHandler flash lỗi và quay lại URL có email.
			if (hasSafeResetReferer(servletRequest, request.getEmail())) {
				throw exception;
			}
			// Fallback giữ email khi Referer thiếu, không hợp lệ hoặc trỏ tới trang khác.
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
			return "redirect:/auth/reset-password?email=" + encodeEmail(request.getEmail());
		}

		redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công, vui lòng đăng nhập");
		return "redirect:/auth/login";
	}

	private boolean hasSafeResetReferer(HttpServletRequest request, String email) {
		String referer = request.getHeader("Referer");
		if (referer == null || referer.isBlank()) {
			return false;
		}
		try {
			URI uri = URI.create(referer);
			if (!"/auth/reset-password".equals(uri.getRawPath())
					|| !("email=" + encodeEmail(email)).equals(uri.getRawQuery())) {
				return false;
			}
			if (!uri.isAbsolute()) {
				return uri.getRawAuthority() == null;
			}
			int port = uri.getPort() >= 0 ? uri.getPort()
					: ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
			return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					&& uri.getUserInfo() == null
					&& uri.getHost() != null
					&& uri.getHost().equalsIgnoreCase(request.getServerName())
					&& port == request.getServerPort();
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private void sanitizeResetPasswordRequest(ResetPasswordRequest request, BindingResult bindingResult,
			Model model) {
		request.setOtp(null);
		request.setNewPassword(null);
		request.setConfirmPassword(null);
		BeanPropertyBindingResult safeBindingResult =
				new BeanPropertyBindingResult(request, "resetPasswordRequest");
		for (ObjectError error : bindingResult.getAllErrors()) {
			if (error instanceof FieldError fieldError
					&& ("otp".equals(fieldError.getField())
							|| "newPassword".equals(fieldError.getField())
							|| "confirmPassword".equals(fieldError.getField()))) {
				safeBindingResult.addError(new FieldError(
						"resetPasswordRequest", fieldError.getField(), null, fieldError.isBindingFailure(),
						fieldError.getCodes(), fieldError.getArguments(), fieldError.getDefaultMessage()));
			} else {
				safeBindingResult.addError(error);
			}
		}
		model.addAttribute("resetPasswordRequest", request);
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "resetPasswordRequest", safeBindingResult);
	}

	private ResendOtpRequest resendRequest(String email) {
		ResendOtpRequest request = new ResendOtpRequest();
		request.setEmail(email);
		return request;
	}

	private String encodeEmail(String email) {
		return UriUtils.encodeQueryParam(email.trim().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
	}

	private String loginValidationError(LoginRequest request, BindingResult bindingResult, Model model) {
		request.setPassword(null);
		BeanPropertyBindingResult safeResult = new BeanPropertyBindingResult(request, "loginRequest");
		for (ObjectError error : bindingResult.getAllErrors()) {
			if (error instanceof FieldError fieldError && "password".equals(fieldError.getField())) {
				safeResult.addError(new FieldError("loginRequest", "password", null,
						fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
						fieldError.getDefaultMessage()));
			} else {
				safeResult.addError(error);
			}
		}
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "loginRequest", safeResult);
		return "auth/login";
	}

	private void setAuthCookies(HttpServletResponse response, AuthTokensResponse tokens) {
		cookieUtil.addAccessTokenCookie(response, tokens.accessToken());
		if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
			cookieUtil.addRefreshTokenCookie(response, tokens.refreshToken());
		}
	}

	private Collection<? extends GrantedAuthority> toAuthorities(Collection<RoleName> roles) {
		return roles.stream().map(RoleName::name).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
	}
}
