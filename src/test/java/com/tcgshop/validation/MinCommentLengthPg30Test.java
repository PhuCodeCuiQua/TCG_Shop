package com.tcgshop.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinCommentLengthPg30Test {
	private final MinCommentLengthValidator validator = new MinCommentLengthValidator();

	@Test
	void rejectsFortyNineCharacters() {
		assertThat(validator.isValid("x".repeat(49), null)).isFalse();
	}

	@Test
	void acceptsFiftyCharacters() {
		assertThat(validator.isValid("x".repeat(50), null)).isTrue();
	}

	@Test
	void rejectsWhitespaceOnlyComment() {
		assertThat(validator.isValid("    ", null)).isFalse();
	}

	@Test
	void acceptsAnActuallyEmptyOptionalComment() {
		assertThat(validator.isValid("", null)).isTrue();
		assertThat(validator.isValid(null, null)).isTrue();
	}
}
