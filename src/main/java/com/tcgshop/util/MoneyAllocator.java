package com.tcgshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chia tổng tiền nguyên đồng theo trọng số bằng phương pháp phần dư lớn nhất. */
public final class MoneyAllocator {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal ONE = BigDecimal.ONE;

	private MoneyAllocator() {
	}

	public static List<BigDecimal> allocateProportionally(BigDecimal total, List<BigDecimal> weights) {
		BigDecimal normalizedTotal = requireNonNegativeInteger(total, "total");
		if (weights == null || weights.isEmpty()) {
			throw new IllegalArgumentException("weights không được null hoặc rỗng");
		}

		List<BigDecimal> normalizedWeights = new ArrayList<>(weights.size());
		BigDecimal weightSum = ZERO;
		for (int index = 0; index < weights.size(); index++) {
			BigDecimal weight = requireNonNegativeInteger(weights.get(index), "weights[" + index + "]");
			normalizedWeights.add(weight);
			weightSum = weightSum.add(weight);
		}

		if (weightSum.signum() == 0) {
			if (normalizedTotal.signum() != 0) {
				throw new IllegalArgumentException("total phải bằng 0 khi tổng weights bằng 0");
			}
			return zeroShares(weights.size());
		}
		if (normalizedTotal.compareTo(weightSum) > 0) {
			throw new IllegalArgumentException("total không được lớn hơn tổng weights");
		}

		List<BigDecimal> shares = new ArrayList<>(weights.size());
		List<BigDecimal> remainders = new ArrayList<>(weights.size());
		BigDecimal allocated = ZERO;
		for (BigDecimal weight : normalizedWeights) {
			BigDecimal numerator = normalizedTotal.multiply(weight);
			BigDecimal[] quotientAndRemainder = numerator.divideAndRemainder(weightSum);
			BigDecimal share = quotientAndRemainder[0].setScale(0, RoundingMode.UNNECESSARY);
			shares.add(share);
			remainders.add(quotientAndRemainder[1]);
			allocated = allocated.add(share);
		}

		// Chia từng đồng còn dư cho phần có số dư lớn nhất; khi hòa, chỉ số nhỏ hơn được ưu tiên.
		int remainingUnits = normalizedTotal.subtract(allocated).intValueExact();
		List<Integer> priority = new ArrayList<>(weights.size());
		for (int index = 0; index < weights.size(); index++) {
			priority.add(index);
		}
		priority.sort(Comparator.<Integer, BigDecimal>comparing(remainders::get)
				.reversed().thenComparingInt(Integer::intValue));
		for (int index = 0; index < remainingUnits; index++) {
			int recipient = priority.get(index);
			shares.set(recipient, shares.get(recipient).add(ONE));
		}

		// Vì total không vượt tổng trọng số, quota chính xác của từng mục không vượt weight_i;
		// phép lấy floor và việc chỉ bù các phần lẻ lớn nhất cũng không làm share_i vượt weight_i.
		return List.copyOf(shares);
	}

	private static BigDecimal requireNonNegativeInteger(BigDecimal value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " không được null");
		}
		final BigDecimal integerValue;
		try {
			integerValue = value.setScale(0, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(name + " phải là số nguyên đồng", exception);
		}
		if (integerValue.signum() < 0) {
			throw new IllegalArgumentException(name + " không được âm");
		}
		return integerValue;
	}

	private static List<BigDecimal> zeroShares(int size) {
		List<BigDecimal> shares = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			shares.add(ZERO);
		}
		return List.copyOf(shares);
	}
}
