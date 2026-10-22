package com.tcgshop.service;

import java.util.List;

import com.tcgshop.entity.enums.OtpPurpose;

public interface EmailService {
	void sendOtpEmail(String to, String fullName, String otp, OtpPurpose purpose, int validMinutes);

	void sendNotificationEmail(String to, String subject, String heading, List<String> paragraphs);
}
