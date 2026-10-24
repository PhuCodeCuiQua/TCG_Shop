package com.tcgshop.repository.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderSpecificationsTest {
	@Test
	void escapesSqlLikeWildcardsAndEscapeCharacter() {
		assertEquals("50\\%\\_off\\\\tail", OrderSpecifications.escapeLike("50%_off\\tail"));
	}
}
