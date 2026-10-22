package com.tcgshop.controller.web;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

	public AuthController(AuthService authService, OtpProperties otpProperties) {
		this.authService = authService;
		this.otpProperties = otpProperties;
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

	private ResendOtpRequest resendRequest(String email) {
		ResendOtpRequest request = new ResendOtpRequest();
		request.setEmail(email);
		return request;
	}

	private String encodeEmail(String email) {
		return UriUtils.encodeQueryParam(email.trim().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
	}
}
