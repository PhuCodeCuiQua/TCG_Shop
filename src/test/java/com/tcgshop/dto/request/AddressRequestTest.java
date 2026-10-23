package com.tcgshop.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AddressRequestTest {
	private static jakarta.validation.ValidatorFactory validatorFactory;
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
	void acceptsVietnameseReceiverNameAndValidAddress() {
		assertTrue(validator.validate(validRequest()).isEmpty());
	}

	@Test
	void rejectsDigitsInNameInvalidPhoneAndMissingLocationFields() {
		AddressRequest request = validRequest();
		request.setReceiverName("Nguyễn An 2");
		request.setPhoneNumber("12345");
		request.setProvince(" ");
		request.setDistrict(null);
		request.setWard("");

		Set<String> fields = validator.validate(request).stream()
				.map(error -> error.getPropertyPath().toString()).collect(Collectors.toSet());

		assertTrue(fields.containsAll(Set.of("receiverName", "phoneNumber", "province", "district", "ward")));
	}

	@Test
	void rejectsOverlongAddressValues() {
		AddressRequest request = validRequest();
		request.setDetailAddress("x".repeat(256));
		request.setProvince("x".repeat(101));

		Set<String> fields = validator.validate(request).stream()
				.map(error -> error.getPropertyPath().toString()).collect(Collectors.toSet());

		assertFalse(fields.isEmpty());
		assertTrue(fields.containsAll(Set.of("detailAddress", "province")));
	}

	private AddressRequest validRequest() {
		AddressRequest request = new AddressRequest();
		request.setReceiverName("Nguyễn Văn An");
		request.setPhoneNumber("0912345678");
		request.setProvince("Hồ Chí Minh");
		request.setDistrict("Quận 1");
		request.setWard("Phường 1");
		request.setDetailAddress("12 Đường A");
		return request;
	}
}
