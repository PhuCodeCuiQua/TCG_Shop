package com.tcgshop.service.impl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {
	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
	private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final String senderEmail;
	private final String senderName;

	public EmailServiceImpl(
			JavaMailSender mailSender,
			TemplateEngine templateEngine,
			@Value("${spring.mail.username}") String senderEmail,
			@Value("${app.mail.sender-name}") String senderName) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.senderEmail = senderEmail;
		this.senderName = senderName;
	}

	@Override
	// Gửi mail ở luồng nền để SMTP chậm không giữ request đăng ký/đặt lại mật khẩu.
	@Async("mailExecutor")
	public void sendOtpEmail(String to, String fullName, String otp, OtpPurpose purpose, int validMinutes) {
		String subject = purpose == OtpPurpose.REGISTER
				? "Xác nhận đăng ký tài khoản"
				: "Đặt lại mật khẩu";

		Context context = new Context(VIETNAMESE);
		context.setVariable("fullName", fullName);
		context.setVariable("otp", otp);
		context.setVariable("validMinutes", validMinutes);
		context.setVariable("heading", subject);
		sendHtml(to, subject, "email/otp", context, "OTP");
	}

	@Override
	// Dùng chung executor nền cho các email thông báo để request nghiệp vụ không phải chờ SMTP.
	@Async("mailExecutor")
	public void sendNotificationEmail(String to, String subject, String heading, List<String> paragraphs) {
		Context context = new Context(VIETNAMESE);
		context.setVariable("heading", heading);
		context.setVariable("paragraphs", paragraphs);
		sendHtml(to, subject, "email/notification", context, "thông báo");
	}

	@Override
	public void sendTemporaryPasswordEmail(
			String to, String fullName, String temporaryPassword, String roleDisplayName) {
		String subject = "Thông tin đăng nhập tài khoản nhân sự";
		Context context = new Context(VIETNAMESE);
		context.setVariable("heading", subject);
		context.setVariable("paragraphs", List.of(
				"Xin chào " + fullName + ",",
				"Tài khoản " + roleDisplayName + " của bạn đã được tạo.",
				"Mật khẩu tạm thời: " + temporaryPassword,
				"Vui lòng đăng nhập và đổi mật khẩu ngay trong lần truy cập đầu tiên."));
		try {
			sendHtmlOrThrow(to, subject, "email/notification", context);
		} catch (MessagingException | UnsupportedEncodingException | RuntimeException exception) {
			log.error("Gửi email mật khẩu tạm thời thất bại ({})", exception.getClass().getSimpleName());
			throw new BusinessException(
					"Không thể gửi email thông tin đăng nhập; tài khoản chưa được tạo thành công");
		}
	}

	private void sendHtml(String to, String subject, String template, Context context, String mailType) {
		try {
			sendHtmlOrThrow(to, subject, template, context);
		} catch (MessagingException | UnsupportedEncodingException | MailSendException exception) {
			// Chỉ log loại lỗi; không ghi OTP, nội dung thư hay địa chỉ người nhận.
			log.error("Gửi email {} thất bại ({})", mailType, exception.getClass().getSimpleName());
		} catch (RuntimeException exception) {
			// Lỗi kết xuất template cũng chỉ được ghi log; không làm gián đoạn luồng gọi.
			log.error("Chuẩn bị email {} thất bại ({})", mailType, exception.getClass().getSimpleName());
		}
	}

	private void sendHtmlOrThrow(String to, String subject, String template, Context context)
			throws MessagingException, UnsupportedEncodingException {
		String html = templateEngine.process(template, context);
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
		helper.setTo(to);
		helper.setFrom(senderEmail, senderName);
		helper.setSubject(subject);
		helper.setText(html, true);
		mailSender.send(message);
	}
}
