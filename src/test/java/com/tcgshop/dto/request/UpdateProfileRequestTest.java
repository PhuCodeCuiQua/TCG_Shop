package com.tcgshop.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateProfileRequestTest {

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
	void rejectsNameContainingDigitsOrSpecialCharacters() {
		assertFalse(isValid("An 2", "0912345678"));
		assertFalse(isValid("An @", "0912345678"));
	}

	@Test
	void acceptsVietnameseNameWithDiacritics() {
		assertTrue(isValid("Nguyễn Văn An", "0912345678"));
	}

	@Test
	void rejectsInvalidPhoneNumber() {
		assertFalse(isValid("Nguyễn Văn An", "12345"));
	}

	private boolean isValid(String fullName, String phoneNumber) {
		UpdateProfileRequest request = new UpdateProfileRequest();
		request.setFullName(fullName);
		request.setPhoneNumber(phoneNumber);
		return validator.validate(request).isEmpty();
	}
}
