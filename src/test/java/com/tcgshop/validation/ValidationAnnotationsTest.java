package com.tcgshop.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ValidationAnnotationsTest {
	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void reviewCommentsAllowShortOrMissingTextAndKeepMaximumLength() {
		for (String comment : new String[] {null, "", "   ", "a", "ok"}) {
			assertTrue(validator.validate(new com.tcgshop.dto.request.ReviewSubmitRequest(5, comment)).isEmpty());
			var media = new com.tcgshop.dto.request.ReviewMediaRequest();
			media.setRating(5);
			media.setComment(comment);
			assertTrue(validator.validate(media).isEmpty());
		}
		assertEquals(1, validator.validate(new com.tcgshop.dto.request.ReviewSubmitRequest(5, "a".repeat(2001))).size());
	}

	@Test
	void strongPasswordAcceptsPasswordMeetingAllRules() {
		assertTrue(validator.validate(new PasswordForm("Abcdefg1")).isEmpty());
	}

	@Test
	void strongPasswordRejectsShortOrIncompletePassword() {
		assertEquals(1, validator.validate(new PasswordForm("abcdefg1")).size());
		assertEquals(1, validator.validate(new PasswordForm("Abc1")).size());
	}

	@Test
	void strongPasswordRejectsValuesLongerThan64Characters() {
		assertEquals(1, validator.validate(new PasswordForm("A" + "a".repeat(63) + "1")).size());
	}

	@Test
	void fieldMatchAcceptsEqualAndRejectsDifferentFields() {
		assertTrue(validator.validate(new PasswordConfirmation("Abcdefg1", "Abcdefg1")).isEmpty());
		Set<?> violations = validator.validate(new PasswordConfirmation("Abcdefg1", "Different1"));
		assertEquals(1, violations.size());
	}

	static class PasswordForm {
		@StrongPassword
		private final String password;

		PasswordForm(String password) {
			this.password = password;
		}

		public String getPassword() {
			return password;
		}
	}

	@FieldMatch(first = "password", second = "confirmPassword", message = "Mật khẩu xác nhận không khớp")
	static class PasswordConfirmation {
		private final String password;
		private final String confirmPassword;

		PasswordConfirmation(String password, String confirmPassword) {
			this.password = password;
			this.confirmPassword = confirmPassword;
		}

		public String getPassword() {
			return password;
		}

		public String getConfirmPassword() {
			return confirmPassword;
		}
	}
}
