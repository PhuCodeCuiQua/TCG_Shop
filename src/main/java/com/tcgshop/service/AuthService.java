package com.tcgshop.service;

import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;

public interface AuthService {
	void register(RegisterRequest request);

	void verifyRegistration(VerifyOtpRequest request);

	void resendRegistrationOtp(ResendOtpRequest request);
}
