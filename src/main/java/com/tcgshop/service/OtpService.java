package com.tcgshop.service;

import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;

public interface OtpService {
	void issue(User user, OtpPurpose purpose);

	void verify(User user, OtpPurpose purpose, String otp);
}
