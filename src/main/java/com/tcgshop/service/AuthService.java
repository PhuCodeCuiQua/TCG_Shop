package com.tcgshop.service;

import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.request.ChangePasswordRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;
import com.tcgshop.dto.response.AuthTokensResponse;

public interface AuthService {
	void register(RegisterRequest request);

	void verifyRegistration(VerifyOtpRequest request);

	void resendRegistrationOtp(ResendOtpRequest request);

	AuthTokensResponse login(LoginRequest request);

	void logout(String refreshTokenRaw);

	AuthTokensResponse changePassword(ChangePasswordRequest request);
}
