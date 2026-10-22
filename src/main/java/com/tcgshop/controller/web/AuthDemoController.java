package com.tcgshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tcgshop.config.OtpProperties;
import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;

@Controller
@RequestMapping("/demo/auth")
public class AuthDemoController {
	private final OtpProperties otpProperties;

	public AuthDemoController(OtpProperties otpProperties) {
		this.otpProperties = otpProperties;
	}

	@GetMapping("/login")
	public String showLogin(Model model) {
		model.addAttribute("loginRequest", new DemoAuthForm());
		return "auth/login";
	}

	@GetMapping("/register")
	public String showRegister(Model model) {
		model.addAttribute("registerRequest", new RegisterRequest());
		return "auth/register";
	}

	@GetMapping("/verify-otp")
	public String showVerifyOtp(@RequestParam(required = false) String email, Model model) {
		VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
		verifyRequest.setEmail(email);
		ResendOtpRequest resendRequest = new ResendOtpRequest();
		resendRequest.setEmail(email);
		model.addAttribute("verifyOtpRequest", verifyRequest);
		model.addAttribute("resendOtpRequest", resendRequest);
		model.addAttribute("validMinutes", otpProperties.validMinutes());
		return "auth/verify-otp";
	}

	@GetMapping("/forgot-password")
	public String showForgotPassword(Model model) {
		model.addAttribute("forgotPasswordRequest", new DemoAuthForm());
		return "auth/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPassword(Model model) {
		model.addAttribute("resetPasswordRequest", new DemoAuthForm());
		return "auth/reset-password";
	}

	public static class DemoAuthForm {
		private String email;
		private String password;
		private String otp;
		private String newPassword;
		private String confirmPassword;

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getOtp() {
			return otp;
		}

		public void setOtp(String otp) {
			this.otp = otp;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}

		public String getConfirmPassword() {
			return confirmPassword;
		}

		public void setConfirmPassword(String confirmPassword) {
			this.confirmPassword = confirmPassword;
		}
	}
}
