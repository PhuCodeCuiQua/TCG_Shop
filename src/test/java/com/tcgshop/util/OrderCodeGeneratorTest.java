package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class OrderCodeGeneratorTest {
	private static final String ALLOWED = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private final OrderCodeGenerator generator = new OrderCodeGenerator();

	@Test
	void generatesOrderAndGroupCodesWithExpectedFormatAndAlphabet() {
		assertCode(generator.generateOrderCode(), "TCG");
		assertCode(generator.generateGroupCode(), "GRP");
	}

	@Test
	void retriesCollisionAndReturnsCodeWhenNextAttemptIsAvailable() {
		AtomicInteger attempts = new AtomicInteger();

		String code = generator.generateUnique("TCG", ignored -> attempts.getAndIncrement() == 0);

		assertCode(code, "TCG");
		assertEquals(2, attempts.get());
	}

	@Test
	void throwsAfterFiveCollisions() {
		AtomicInteger attempts = new AtomicInteger();

		assertThrows(IllegalStateException.class,
				() -> generator.generateUnique("GRP", ignored -> {
					attempts.incrementAndGet();
					return true;
				}));

		assertEquals(5, attempts.get());
	}

	private static void assertCode(String code, String prefix) {
		String date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("yyMMdd"));
		assertTrue(code.matches("^" + prefix + date + "[A-Z2-9]{6}$"));
		assertEquals(prefix.length() + 12, code.length());
		for (char character : code.substring(prefix.length() + 6).toCharArray()) {
			assertFalse(ALLOWED.indexOf(character) < 0, "Unexpected code character: " + character);
		}
	}
}
