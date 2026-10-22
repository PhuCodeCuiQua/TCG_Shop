package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyUtilsTest {

	@Test
	void roundUsesHalfUpAtVndScale() {
		assertEquals(new BigDecimal("2"), MoneyUtils.round(new BigDecimal("1.5")));
		assertEquals(new BigDecimal("-2"), MoneyUtils.round(new BigDecimal("-1.5")));
	}

	@Test
	void formatUsesVietnameseThousandsSeparatorsAndHandlesNull() {
		assertEquals("1.234.567 ₫", MoneyUtils.format(new BigDecimal("1234567.4")));
		assertEquals("0 ₫", MoneyUtils.format(null));
	}

	@Test
	void arithmeticTreatsNullAsZeroAndNonNegativeClampsBelowZero() {
		assertEquals(new BigDecimal("5"), MoneyUtils.add(null, new BigDecimal("5")));
		assertEquals(new BigDecimal("5"), MoneyUtils.subtract(new BigDecimal("5"), null));
		assertEquals(BigDecimal.ZERO, MoneyUtils.nonNegative(new BigDecimal("-0.01")));
		assertEquals(new BigDecimal("2.50"), MoneyUtils.nonNegative(new BigDecimal("2.50")));
	}
}
