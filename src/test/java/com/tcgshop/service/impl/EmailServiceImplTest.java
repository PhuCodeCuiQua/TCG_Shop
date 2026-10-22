package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import com.tcgshop.entity.enums.OtpPurpose;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
	@Mock
	private JavaMailSender mailSender;

	@Mock
	private TemplateEngine templateEngine;

	private EmailServiceImpl emailService;

	@BeforeEach
	void setUp() {
		emailService = new EmailServiceImpl(mailSender, templateEngine, "sender@example.com", "TCG Shop");
	}

	@Test
	void sendsOtpEmailToRequestedRecipientOnce() throws Exception {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		when(templateEngine.process(any(String.class), any(org.thymeleaf.context.Context.class)))
				.thenReturn("<html>OTP</html>");
		when(mailSender.createMimeMessage()).thenReturn(message);

		emailService.sendOtpEmail("member@example.com", "Người dùng", "123456", OtpPurpose.REGISTER, 5);

		verify(mailSender).send(message);
		org.junit.jupiter.api.Assertions.assertEquals("member@example.com", message.getAllRecipients()[0].toString());
	}

	@Test
	void mailSendExceptionDoesNotEscapeServiceMethod() throws Exception {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		when(templateEngine.process(any(String.class), any(org.thymeleaf.context.Context.class)))
				.thenReturn("<html>OTP</html>");
		when(mailSender.createMimeMessage()).thenReturn(message);
		doThrow(new MailSendException("test failure")).when(mailSender).send(any(MimeMessage.class));

		assertDoesNotThrow(() -> emailService.sendOtpEmail(
				"member@example.com", "Người dùng", "123456", OtpPurpose.FORGOT_PASSWORD, 5));
		verify(mailSender).send(message);
	}
}
