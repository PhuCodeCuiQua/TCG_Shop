package com.tcgshop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, CharSequence> {
	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null || value.length() < 8 || value.length() > 64) {
			return false;
		}

		boolean hasUppercase = false;
		boolean hasLowercase = false;
		boolean hasDigit = false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			hasUppercase |= Character.isUpperCase(character);
			hasLowercase |= Character.isLowerCase(character);
			hasDigit |= character >= '0' && character <= '9';
		}
		return hasUppercase && hasLowercase && hasDigit;
	}
}
