package com.tcgshop.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinCommentLengthValidatorTest {
	private final MinCommentLengthValidator validator = new MinCommentLengthValidator();

	@Test
	void allowsNullAndEmptyCommentsButRejectsWhitespaceOnly() {
		assertThat(validator.isValid(null, null)).isTrue();
		assertThat(validator.isValid("", null)).isTrue();
		assertThat(validator.isValid("   ", null)).isFalse();
	}

	@Test
	void requiresAtLeastFiftyNonWhitespaceCharactersWhenCommentIsPresent() {
		assertThat(validator.isValid("x".repeat(49), null)).isFalse();
		assertThat(validator.isValid("x".repeat(50), null)).isTrue();
		assertThat(validator.isValid("  " + "x".repeat(49) + "  ", null)).isFalse();
	}
}
