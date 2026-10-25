package com.tcgshop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinCommentLengthValidator implements ConstraintValidator<MinCommentLength, CharSequence> {
	private static final int MINIMUM_LENGTH = 50;

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		return value == null || value.toString().isBlank()
				|| value.toString().trim().length() >= MINIMUM_LENGTH;
	}
}
