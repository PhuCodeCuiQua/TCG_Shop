package com.tcgshop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinCommentLengthValidator implements ConstraintValidator<MinCommentLength, CharSequence> {
	private static final int MINIMUM_LENGTH = 50;

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null || value.length() == 0) {
			return true;
		}
		return value.toString().trim().length() >= MINIMUM_LENGTH;
	}
}
