package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MoneyAllocatorTest {
	@Test
	void distributesByLargestRemainderAndUsesIndexForTies() {
		assertEquals(values(1, 2, 2), MoneyAllocator.allocateProportionally(value(5), values(1, 2, 3)));
		assertEquals(values(1, 1, 0), MoneyAllocator.allocateProportionally(value(2), values(1, 1, 1)));
	}

	@Test
	void handlesZeroTotalFullTotalAndZeroWeights() {
		assertEquals(values(0, 0, 0), MoneyAllocator.allocateProportionally(value(0), values(4, 0, 7)));
		assertEquals(values(4, 0, 7), MoneyAllocator.allocateProportionally(value(11), values(4, 0, 7)));
		assertEquals(BigDecimal.ZERO, MoneyAllocator.allocateProportionally(value(1), values(0, 2)).get(0));
		assertEquals(values(0, 0), MoneyAllocator.allocateProportionally(value(0), values(0, 0)));
	}

	@Test
	void rejectsInvalidInputs() {
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(6), values(1, 2, 2)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(1), values(0, 0)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(0), values(-1, 2)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(0), java.util.Arrays.asList(value(1), null)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(-1), values(1, 2)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(new BigDecimal("1.5"), values(1, 2)));
		assertThrows(IllegalArgumentException.class,
				() -> MoneyAllocator.allocateProportionally(value(0), List.of()));
	}

	@Test
	void preservesBoundsAndTotalAcrossDeterministicRandomCases() {
		Random random = new Random(0x13A5EEDL);
		for (int iteration = 0; iteration < 2_000; iteration++) {
			int size = 1 + random.nextInt(8);
			List<BigDecimal> weights = new ArrayList<>(size);
			long weightSum = 0;
			for (int index = 0; index < size; index++) {
				long weight = random.nextInt(4) == 0 ? 0 : random.nextInt(1_000_001);
				weights.add(value(weight));
				weightSum += weight;
			}
			long total = weightSum == 0 ? 0 : random.nextLong(weightSum + 1);
			List<BigDecimal> shares = MoneyAllocator.allocateProportionally(value(total), weights);

			assertEquals(value(total), shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
					"Tổng phân bổ phải bằng total tại vòng " + iteration);
			for (int index = 0; index < size; index++) {
				assertTrue(shares.get(index).signum() >= 0,
						"Phần phân bổ không được âm tại vòng " + iteration);
				assertTrue(shares.get(index).compareTo(weights.get(index)) <= 0,
						"Phần phân bổ không được vượt trọng số tại vòng " + iteration);
				assertEquals(0, shares.get(index).scale(), "Scale kết quả phải bằng 0");
			}
		}
	}

	private static BigDecimal value(long amount) {
		return BigDecimal.valueOf(amount);
	}

	private static List<BigDecimal> values(long... amounts) {
		return java.util.Arrays.stream(amounts).mapToObj(BigDecimal::valueOf).toList();
	}
}
