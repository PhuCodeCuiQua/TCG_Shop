package com.tcgshop.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tcgshop.event.OtpIssuedEvent;
import com.tcgshop.service.EmailService;

@Component
public class OtpEmailListener {
	private static final Logger log = LoggerFactory.getLogger(OtpEmailListener.class);

	private final EmailService emailService;

	public OtpEmailListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOtpIssued(OtpIssuedEvent event) {
		try {
			emailService.sendOtpEmail(
					event.email(), event.fullName(), event.otp(), event.purpose(), event.validMinutes());
		} catch (RuntimeException exception) {
			// Không ghi mã OTP hoặc thông tin nhạy cảm; lỗi gửi mail không được làm hỏng nghiệp vụ đã commit.
			log.error("Không thể khởi chạy tác vụ gửi email OTP ({})", exception.getClass().getSimpleName());
		}
	}
}
